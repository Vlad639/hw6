package com.github.javarar.poke;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@RestController
public class GetPokemons {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Random random = new SecureRandom();
    private final Map<String, CompletableFuture<List<Response>>> tasks = new HashMap<>();

    public static long ASYNC_TASK_TIMEOUT_MLS = 60_000;
    public static String POKEMON_URL = "https://pokeapi.co/api/v2/pokemon/";

    //http://localhost:8080/getAll?names=ditto,pikachu,charmander,bulbasaur
    @GetMapping("/getAll") // Все запрошенные покемоны синхронно
    @SuppressWarnings("unused")
    public String getAll(@RequestParam(value = "names") List<String> names) throws IOException {
        if (names == null || names.size() == 0) {
            return jsonMessage("Names not specified");
        }

        List<Response> response = new ArrayList<>();
        for (String name : names) {
            response.add(getPokemonInfo(name));
        }

        return mapper.writeValueAsString(response);
    }

    //http://localhost:8080/oneOf?names=ditto,pikachu,charmander,bulbasaur
    @GetMapping("/oneOf") // Один из запрошенных покемонов синхронно
    @SuppressWarnings("unused")
    public String oneOf(@RequestParam(value = "names") List<String> names) throws IOException {
        if (names == null || names.size() == 0) {
            return jsonMessage("Names not specified");
        }
        String randomName = names.get(random.nextInt(names.size()));
        return mapper.writeValueAsString(getPokemonInfo(randomName));
    }

    //http://localhost:8080/getAllParallel?names=ditto,pikachu,charmander,bulbasaur
    @GetMapping("/getAllParallel")
    @SuppressWarnings("unused") // Все запрошенные покемоны, запрос к сервису параллельно, сам ответ синхронный
    public String getAllParallel(@RequestParam(value = "names") List<String> names) throws IOException, ExecutionException, InterruptedException {
        if (names == null || names.size() == 0) {
            return jsonMessage("Names not specified");
        }

        CompletableFuture<List<Response>> responseFuture = getAsync(names);
        return mapper.writeValueAsString(responseFuture.get());
    }

    //http://localhost:8080/getAnyOf?names=pikachu,charmander,bulbasaur
    @GetMapping("/getAnyOf") // Первый из вернувшихся покемонов, запрос к сервису параллельно, сам ответ синхронный
    @SuppressWarnings("unused")
    public String anyOf(@RequestParam(value = "names") List<String> names) throws IOException, ExecutionException, InterruptedException {
        if (names == null || names.size() == 0) {
            return jsonMessage("Names not specified");
        }

        List<CompletableFuture<Response>> tasks = new ArrayList<>();
        for (String name : names) {
            tasks.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return getPokemonInfo(name);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        CompletableFuture<Object> resultFuture = CompletableFuture.anyOf(tasks.toArray(new CompletableFuture[0]));

        return mapper.writeValueAsString(resultFuture.get());
    }

    //http://localhost:8080/getAsync?names=ditto,pikachu,charmander,bulbasaur
    @GetMapping("/getAsync") // Получить покемонов асинхронно
    @SuppressWarnings("unused")
    public String getAsync(
            @RequestParam(value = "names", required = false) List<String> names,
            @RequestParam(value = "id", required = false) String id)
            throws IOException, ExecutionException, InterruptedException {

        if ((names == null || names.size() == 0) && StringUtils.isEmpty(id)) {
            return jsonMessage("Names or id not specified");
        }

        if ((names != null && !names.isEmpty() && StringUtils.isNotEmpty(id))) {
            return jsonMessage("Specify only names or only id", id);
        }

        if (names != null && !names.isEmpty()) {
            String rqId = UUID.randomUUID().toString();
            CompletableFuture<List<Response>> task = CompletableFuture.supplyAsync(() -> {
                try {
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (tasks.containsKey(rqId)) {
                                tasks.get(rqId).cancel(true);
                                tasks.remove(rqId);
                            }
                        }
                    }, ASYNC_TASK_TIMEOUT_MLS);
                    return getAsync(names).get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });

            tasks.put(rqId, task);
            return jsonMessage("Async task register", rqId);
        }

        if (StringUtils.isNotEmpty(id)) {
            if (!tasks.containsKey(id)) {
                return jsonMessage("Task is not register", id);
            }

            CompletableFuture<List<Response>> task = tasks.get(id);
            if (!task.isDone()) {
                return jsonMessage("Task is not complete yet", id);
            }

            if (task.isCancelled()) {
                tasks.remove(id);
                return jsonMessage("Task is canceled", id);
            }

            if (task.isDone()) {
                tasks.remove(id);
                return mapper.writeValueAsString(task.get());
            }
        }

        return jsonMessage("OK");
    }

    private CompletableFuture<List<Response>> getAsync(List<String> names) {
        List<CompletableFuture<Response>> tasks = new ArrayList<>();
        for (String name : names) {
            tasks.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return getPokemonInfo(name);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        CompletableFuture<Void> allOf = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));
        return allOf.thenApply(t -> tasks.stream().map(CompletableFuture::join).collect(Collectors.toList()));
    }

    private Response getPokemonInfo(String name) throws IOException {
        String result = restTemplate.getForEntity(POKEMON_URL + name, String.class).getBody();
        Map<String, Object> mapResult = mapper.createParser(result).readValueAs(Map.class);
        return mapToResponse(mapResult);
    }

    private Response mapToResponse(Map<String, Object> map) {
        Response response = new Response();
        response.setName((String) map.get("name"));
        response.setHeight(Double.valueOf(map.get("height").toString()));
        response.setWeight(Double.valueOf(map.get("weight").toString()));

        List<String> abilities = new ArrayList<>();
        List<Map<String, Object>> rawAbilities = (List<Map<String, Object>>) map.get("abilities");
        if (rawAbilities == null) {
            return response;
        }

        for (Map<String, Object> rawAbility : rawAbilities) {
            Map<String, Object> ability = (Map<String, Object>) rawAbility.get("ability");
            abilities.add((String) ability.get("name"));

        }

        response.setAbilities(abilities);
        return response;
    }

    private String jsonMessage(String message, String id) throws JsonProcessingException {
        Map<String, String> json = new LinkedHashMap<>();
        json.put("message", message);
        json.put("id", id);
        return mapper.writeValueAsString(json);
    }

    private String jsonMessage(String message) throws JsonProcessingException {
        return mapper.writeValueAsString(Collections.singletonMap("message", message));
    }
}
