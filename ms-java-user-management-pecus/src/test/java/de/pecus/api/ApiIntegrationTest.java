package de.pecus.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * End-to-end tests of the REST API against a real PostgreSQL created from
 * src/main/resources/db/schema.sql (the same script applied to Supabase).
 * The requests mirror what the HackTheSnack frontend sends.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ApiIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withInitScript("db/schema.sql");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE result_item, evaluation, recipe, product, substance RESTART IDENTITY CASCADE");
    }

    // ------------------------------------------------------------------ helpers

    private JsonNode call(org.springframework.test.web.servlet.RequestBuilder request, int expectedStatus) throws Exception {
        MvcResult result = mvc.perform(request).andExpect(status().is(expectedStatus)).andReturn();
        String body = result.getResponse().getContentAsString();
        return body.isEmpty() ? null : json.readTree(body);
    }

    private long createIngredient(String name) throws Exception {
        return call(post("/ingredient").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"description\":\"" + name + "\"}"), 201).get("data").asLong();
    }

    private long createProduct(String name) throws Exception {
        return call(post("/product").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"descripcion\":\"" + name + "\",\"idBrand\":1,\"idCategory\":1,\"idSubCategory\":1}"), 201)
                .get("data").asLong();
    }

    private long associate(long productId, long ingredientId) throws Exception {
        return call(post("/recipe").contentType(MediaType.APPLICATION_JSON)
                .content("{\"idProduct\":" + productId + ",\"idIngredient\":" + ingredientId + "}"), 201).get("data").asLong();
    }

    // -------------------------------------------------------------------- tests

    @Test
    void emptyListsAreSuccessfulAndEmpty() throws Exception {
        mvc.perform(get("/product/list?page=1&size=1&name="))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(0)));
        mvc.perform(get("/ingredient/list?page=1&size=500&name="))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void namesLongerThanTwentyCharactersAreKeptAndFindable() throws Exception {
        createIngredient("Partially hydrogenated soybean oil");
        mvc.perform(get("/ingredient/detail").param("name", "PARTIALLY HYDROGENATED SOYBEAN OIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("PARTIALLY HYDROGENATED SOYBEAN OIL")));

        long productId = createProduct("Chocolate Hazelnut Spread Deluxe");
        mvc.perform(get("/product/detail").param("name", "CHOCOLATE HAZELNUT SPREAD DELUXE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is((int) productId)))
                .andExpect(jsonPath("$.data.idBrand", is(1)));
    }

    @Test
    void detailLookupByNameIgnoresCaseAndSurroundingSpaces() throws Exception {
        long ingredientId = createIngredient("Crème fraîche");
        mvc.perform(get("/ingredient/detail").param("name", "  crème Fraîche "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is((int) ingredientId)))
                .andExpect(jsonPath("$.data.name", is("CRÈME FRAÎCHE")));

        long productId = createProduct("M&M's Peanut");
        mvc.perform(get("/product/detail").param("name", "m&m's peanut"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is((int) productId)));
    }

    @Test
    void duplicatesAreRejectedWithBadRequest() throws Exception {
        createIngredient("Sugar");
        mvc.perform(post("/ingredient").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"sugar\",\"description\":\"sugar\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].key", is("DUPLICATED_ERROR")));

        createProduct("Peanut Butter");
        mvc.perform(post("/product").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Peanut Butter\",\"descripcion\":\"x\",\"idBrand\":1,\"idCategory\":1,\"idSubCategory\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void productIdsAreGeneratedSequentially() throws Exception {
        long first = createProduct("First");
        long second = createProduct("Second");
        org.junit.jupiter.api.Assertions.assertTrue(first > 0 && second == first + 1);
    }

    @Test
    void updateKeepingTheSameNameSucceedsAndPreservesCreationDate() throws Exception {
        long productId = createProduct("Granola Bar");
        Timestamp created = jdbc.queryForObject("SELECT dd_fecha_creacion FROM product WHERE pk_id = ?", Timestamp.class, productId);

        mvc.perform(put("/product/" + productId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Granola Bar\",\"descripcion\":\"crunchy\"}"))
                .andExpect(status().isCreated());

        Timestamp after = jdbc.queryForObject("SELECT dd_fecha_creacion FROM product WHERE pk_id = ?", Timestamp.class, productId);
        org.junit.jupiter.api.Assertions.assertEquals(created, after);
        mvc.perform(get("/product/detail").param("id", String.valueOf(productId)))
                .andExpect(jsonPath("$.data.descripcion", is("crunchy")))
                .andExpect(jsonPath("$.data.idBrand", is(1)));
    }

    @Test
    void recipeListContainsIngredientNamesInInsertionOrder() throws Exception {
        long product = createProduct("Peanut Butter");
        long peanuts = createIngredient("Peanuts");
        long salt = createIngredient("Salt");
        associate(product, peanuts);
        associate(product, salt);

        mvc.perform(get("/recipe/list?page=1&size=10&orderBy&orderType&id=" + product))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].nameIngredient", contains("PEANUTS", "SALT")))
                .andExpect(jsonPath("$.data[*].idIngredient", contains((int) peanuts, (int) salt)));
    }

    @Test
    void duplicateAssociationReturns400AndDetailReturnsTheRecipe() throws Exception {
        long product = createProduct("Peanut Butter");
        long peanuts = createIngredient("Peanuts");
        long recipeId = associate(product, peanuts);

        mvc.perform(post("/recipe").contentType(MediaType.APPLICATION_JSON)
                .content("{\"idProduct\":" + product + ",\"idIngredient\":" + peanuts + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].key", is("DUPLICATED_ERROR")));
        mvc.perform(get("/recipe/detailRecipeByProduct?idProduct=" + product + "&idIngredient=" + peanuts))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idRecipe", is((int) recipeId)));
    }

    @Test
    void associationWithUnknownIdsIsRejected() throws Exception {
        long product = createProduct("Peanut Butter");
        mvc.perform(post("/recipe").contentType(MediaType.APPLICATION_JSON)
                .content("{\"idProduct\":" + product + ",\"idIngredient\":999}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/recipe").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removedIngredientDisappearsAndIsReactivatedWithTheSameId() throws Exception {
        long product = createProduct("Peanut Butter");
        long peanuts = createIngredient("Peanuts");
        long salt = createIngredient("Salt");
        associate(product, peanuts);
        long saltRecipe = associate(product, salt);

        mvc.perform(delete("/recipe/deleteByProduct/?idProduct=" + product + "&idIngredient=" + salt))
                .andExpect(status().isOk());
        mvc.perform(get("/recipe/list?id=" + product))
                .andExpect(jsonPath("$.data[*].nameIngredient", contains("PEANUTS")));

        long again = associate(product, salt);
        org.junit.jupiter.api.Assertions.assertEquals(saltRecipe, again);
    }

    @Test
    void evaluationIsStoredWithAllResultItemsAndListedWithNames() throws Exception {
        long product = createProduct("Peanut Butter");
        long peanuts = createIngredient("Peanuts");
        long salt = createIngredient("Salt");
        long r1 = associate(product, peanuts);
        long r2 = associate(product, salt);

        String payload = "{\"idProduct\":" + product + ",\"resultList\":["
                + "{\"recipeId\":" + r1 + ",\"ingredientId\":" + peanuts + ",\"ingredientMeanPercentage\":85.7,\"ingredientStdPercentage\":1.5},"
                + "{\"recipeId\":" + r2 + ",\"ingredientId\":" + salt + ",\"ingredientMeanPercentage\":14.3,\"ingredientStdPercentage\":1.5}]}";
        long evaluationId = call(post("/evaluation").contentType(MediaType.APPLICATION_JSON).content(payload), 201)
                .get("data").asLong();

        mvc.perform(get("/evaluation/detail?id=" + evaluationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idIngredient", is((int) peanuts)))
                .andExpect(jsonPath("$.data.ingredientMeanPercentage", is(85.7)))
                .andExpect(jsonPath("$.data.resultList", hasSize(2)));

        mvc.perform(get("/evaluation/listResult?page=1&size=50&orderBy&orderType&idProduct=" + product + "&idIngredient="))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].ingredientName", contains("PEANUTS", "SALT")))
                .andExpect(jsonPath("$.data[0].productName", is("PEANUT BUTTER")))
                .andExpect(jsonPath("$.data[0].evaluationDate").isString());

        mvc.perform(get("/evaluation/listResult?idProduct=&idIngredient=" + salt))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].ingredientMeanPercentage", is(14.3)));

        mvc.perform(get("/evaluation/list?idProduct=" + product))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void invalidEvaluationsAreRejectedWithoutPartialWrites() throws Exception {
        long product = createProduct("Peanut Butter");
        mvc.perform(post("/evaluation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"idProduct\":" + product + ",\"resultList\":[]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/evaluation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"idProduct\":" + product + ",\"resultList\":[{\"recipeId\":1,\"ingredientId\":12345}]}"))
                .andExpect(status().isBadRequest());
        org.junit.jupiter.api.Assertions.assertEquals(0,
                jdbc.queryForObject("SELECT count(*) FROM evaluation", Integer.class));
    }

    @Test
    void corsAllowsTheGithubPagesOriginOnly() throws Exception {
        mvc.perform(options("/product/list")
                .header("Origin", "https://danilomonge.github.io")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://danilomonge.github.io"));
        mvc.perform(options("/product/list")
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }
}
