package com.github.javarar.poke;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PokeServiceApplicationTests {

    private final RestTemplate restTemplate = new RestTemplate();

    private final static ObjectMapper objectMapper = new JsonMapper();

    private final static Response ditto = new Response("ditto", 25.5, 12.0, null);
    private final static Response pikachu = new Response("pikachu", 15.0,10.0, null);

    private final static Response bulbasaur = new Response("bulbasaur", 25.0,17.5, null);
    private final static Response charmander = new Response("charmander", 30.0,50.0, null);

    private static int appPort;

    @BeforeClass
    public static void before() throws IOException {
        int pokemonServiceStubPort = findFreePort();

        // Поднимаем HTTP-сервер, заглушку сервиса для работы с poke-api
        HttpServer stubServer = HttpServer.create(new InetSocketAddress(pokemonServiceStubPort), 0);
        stubServer.createContext("/ditto", exchange -> handle(exchange, ditto));
        stubServer.createContext("/pikachu", exchange -> handle(exchange, pikachu));
        stubServer.createContext("/bulbasaur", exchange -> {
            try {
                handleWithDelay(exchange, bulbasaur, 3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        stubServer.createContext("/charmander", exchange -> {
            try {
                handleWithDelay(exchange, charmander, 6000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        stubServer.start();

        GetPokemons.ASYNC_TASK_TIMEOUT_MLS = 5000;
        GetPokemons.POKEMON_URL = String.format("http://localhost:%s/", pokemonServiceStubPort);

        appPort = findFreePort();
        //Приложение небольшое, можно его протестить полностью
        PokeServiceApplication.main(new String[] {"--server.port=" + appPort});
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void handleWithDelay(HttpExchange exchange, Response pokemon, long delayMls) throws IOException, InterruptedException {
        Thread.sleep(delayMls);
        handle(exchange, pokemon);
    }

    private static void handle(HttpExchange exchange, Response pokemon) throws IOException {
        String responseString = objectMapper.writeValueAsString(convertPokemonToResponse(pokemon));
        exchange.sendResponseHeaders(200, responseString.length());
        exchange.getResponseBody().write(responseString.getBytes());
        exchange.close();
    }

    private static Map<String, Object> convertPokemonToResponse(Response pokemon) {
        Map<String, Object> response = objectMapper.convertValue(pokemon, Map.class);
        response.put("anotherInfoText", "anotherInfo");
        response.put("anotherInfoNumber", 123);

        return response;
    }


    @Test
    public void getAllEndpointTest() throws IOException {
        List<Response> response = getResponseList("getAll", "ditto", "pikachu");

        assertEquals(2, response.size());
        assertTrue(response.contains(ditto));
        assertTrue(response.contains(pikachu));
    }

    @Test
    public void getAllParallelEndpointTest() throws IOException {
        List<Response> response = getResponseList("getAllParallel", "ditto", "pikachu");

        assertEquals(2, response.size());
        assertTrue(response.contains(ditto));
        assertTrue(response.contains(pikachu));
    }

    @Test
    public void oneOfEndpointTest() throws IOException {
        Response response = getResponse("oneOf", "ditto", "pikachu");
        assertTrue(ditto.equals(response) || pikachu.equals(response));
    }

    @Test
    public void getAnyOfEndpointTest() throws IOException {
        Response response = getResponse("getAnyOf", "ditto", "pikachu");
        assertTrue(ditto.equals(response) || pikachu.equals(response));
    }

    @Test
    public void getAsyncSuccessEndpointTest() throws IOException, InterruptedException {
        String response = getStringResponse("getAsync", "bulbasaur");
        String id = (String) objectMapper.readValue(response, Map.class).get("id");

        assertTrue(getIdResponse(id).contains("not complete yet"));
        Thread.sleep(1000);

        assertTrue(getIdResponse(id).contains("not complete yet"));
        Thread.sleep(2500);

        List<Response> finishResponse= objectMapper.readerForListOf(Response.class).readValue(getIdResponse(id));
        assertEquals(1, finishResponse.size());
        assertEquals(bulbasaur, finishResponse.get(0));

        assertTrue(getIdResponse(id).contains("not register"));
    }

    @Test
    public void getAsyncTimeOutEndpointTest() throws InterruptedException, JsonProcessingException {
        String response = getStringResponse("getAsync", "charmander");
        String id = (String) objectMapper.readValue(response, Map.class).get("id");

        for (int i = 0; i < 5; i++) {
            assertTrue(getIdResponse(id).contains("not complete yet"));
            Thread.sleep(1000);
        }

        Thread.sleep(1500);
        assertTrue(getIdResponse(id).contains("not register"));
    }

    private String getIdResponse(String id) {
        String url = String.format("http://localhost:%s/getAsync?id=%s", appPort, id);

        ResponseEntity<String> responseBody = restTemplate.getForEntity(url, String.class);
        return responseBody.getBody();
    }


    private Response getResponse(String method, String ... names) throws JsonProcessingException {
        String response = getStringResponse(method, names);
        return objectMapper.readValue(response, Response.class);
    }

    private List<Response> getResponseList(String method, String ... names) throws JsonProcessingException {
        String response = getStringResponse(method, names);
        return objectMapper.readerForListOf(Response.class).readValue(response);
    }

    private String getStringResponse(String method, String ... names) {
        StringBuilder url = new StringBuilder(String.format("http://localhost:%s/", appPort));
        url.append(method);
        if (names.length > 0) {
            url.append("?names=").append(String.join(",", names));
        }

        ResponseEntity<String> responseBody = restTemplate.getForEntity(url.toString(), String.class);
        return responseBody.getBody();
    }
}
