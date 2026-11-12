package de.pecus.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import de.pecus.api.controllers.impl.NutritionControllerImpl;
import de.pecus.api.services.nutrition.NutritionService;

/** The endpoints the web app's nutrition dialog calls (same contract as the former proxy). */
class NutritionControllerTest {

    private static MockMvc mvc(NutritionServiceTest.FakeHttp http, String usdaKey, String translatorKey) {
        return MockMvcBuilders.standaloneSetup(
                new NutritionControllerImpl(new NutritionService(http, usdaKey, translatorKey, ""))).build();
    }

    @Test
    void healthAnswersOk() throws Exception {
        mvc(new NutritionServiceTest.FakeHttp(), "", "").perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok", is(true)));
    }

    @Test
    void nutritionReturnsDataOrNull() throws Exception {
        NutritionServiceTest.FakeHttp http = new NutritionServiceTest.FakeHttp();
        http.body = "{\"foods\":[{\"description\":\"SUGAR\",\"foodNutrients\":[{\"nutrientName\":\"Energy\",\"value\":375}]}]}";
        MockMvc mvc = mvc(http, "k", "");

        mvc.perform(get("/api/nutrition").param("ingredient", "sugar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("SUGAR")))
                .andExpect(jsonPath("$.data.kcal", is(375.0)))
                .andExpect(jsonPath("$.data.per", is("100g")));
        mvc.perform(get("/api/nutrition").param("ingredient", "unknown food"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", nullValue()));
    }

    @Test
    void nutritionValidatesInputAndConfiguration() throws Exception {
        mvc(new NutritionServiceTest.FakeHttp(), "", "").perform(get("/api/nutrition").param("ingredient", "sugar"))
                .andExpect(status().isServiceUnavailable());
        mvc(new NutritionServiceTest.FakeHttp(), "k", "").perform(get("/api/nutrition").param("ingredient", " "))
                .andExpect(status().isBadRequest());
        mvc(new NutritionServiceTest.FakeHttp(), "k", "").perform(get("/api/nutrition").param("ingredient", "x".repeat(201)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upstreamFailuresBecome502WithoutLeakingTheKey() throws Exception {
        NutritionServiceTest.FakeHttp http = new NutritionServiceTest.FakeHttp();
        http.status = 403;
        mvc(http, "secret-usda", "").perform(get("/api/nutrition").param("ingredient", "sugar"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail", is("USDA request failed with HTTP 403")))
                .andExpect(jsonPath("$.detail", not(containsString("secret"))));
    }

    @Test
    void translateUsesDefaultsAndReturnsText() throws Exception {
        NutritionServiceTest.FakeHttp http = new NutritionServiceTest.FakeHttp();
        http.body = "[{\"translations\":[{\"text\":\"Sugar\",\"to\":\"en\"}]}]";
        mvc(http, "", "k").perform(post("/api/translate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Zucker\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text", is("Sugar")));
    }
}
