package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.application.AlreadyInHouseholdException;
import io.github.maxziel.storagekeeper.application.NoHouseholdException;
import io.github.maxziel.storagekeeper.application.UnknownInviteCodeException;
import io.github.maxziel.storagekeeper.application.port.in.CreateHouseholdUseCase;
import io.github.maxziel.storagekeeper.application.port.in.JoinHouseholdUseCase;
import io.github.maxziel.storagekeeper.application.port.in.ShowHouseholdUseCase;
import io.github.maxziel.storagekeeper.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HouseholdController.class)
@Import(SecurityConfiguration.class)
@WithMockUser
class HouseholdControllerTest {

    private static final HouseholdMember ALICE = new HouseholdMember(new UserId("id-alice"), "alice");
    private static final HouseholdMember BOB = new HouseholdMember(new UserId("id-bob"), "bob");
    private final Household household =
        Household.found(new HouseholdName("WG Lindenstraße"), ALICE, new InviteCode("K7M2-QX9P"));
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CreateHouseholdUseCase createHousehold;
    @MockitoBean
    private ShowHouseholdUseCase showHousehold;
    @MockitoBean
    private JoinHouseholdUseCase joinHousehold;

    @Test
    void createReturnsLocationHeader() throws Exception {
        when(createHousehold.create(any())).thenReturn(household);

        mockMvc.perform(post("/api/household")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "WG Lindenstraße"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/household"));
    }

    @Test
    void createReturnsTheNewHouseholdWithInviteCode() throws Exception {
        when(createHousehold.create(new HouseholdName("WG Lindenstraße"))).thenReturn(household);

        mockMvc.perform(post("/api/household")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "WG Lindenstraße"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(household.getHouseholdId().toString()))
            .andExpect(jsonPath("$.name").value("WG Lindenstraße"))
            .andExpect(jsonPath("$.inviteCode").value("K7M2-QX9P"))
            .andExpect(jsonPath("$.members[0]").value("alice"));
    }

    @Test
    void createRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/household")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": " "}
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(createHousehold);
    }

    @Test
    void createWhenAlreadyInAHouseholdReturnsConflict() throws Exception {
        when(createHousehold.create(any())).thenThrow(new AlreadyInHouseholdException());

        mockMvc.perform(post("/api/household")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "Zweiter Haushalt"}
                    """))
            .andExpect(status().isConflict());
    }

    @Test
    void showListsTheMembersInJoinOrder() throws Exception {
        household.join(BOB);
        when(showHousehold.getCurrent()).thenReturn(household);

        mockMvc.perform(get("/api/household"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.members[0]").value("alice"))
            .andExpect(jsonPath("$.members[1]").value("bob"));
    }

    @Test
    void showWithoutHouseholdReturnsForbidden() throws Exception {
        when(showHousehold.getCurrent()).thenThrow(new NoHouseholdException());

        mockMvc.perform(get("/api/household"))
            .andExpect(status().isForbidden());
    }

    @Test
    void joinAcceptsTheCodeInLowerCase() throws Exception {
        when(joinHousehold.join(new InviteCode("K7M2-QX9P"))).thenReturn(household);

        mockMvc.perform(post("/api/household/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"inviteCode": "k7m2-qx9p"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("WG Lindenstraße"));
    }

    @Test
    void joinWithMalformedCodeReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/household/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"inviteCode": "abc"}
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(joinHousehold);
    }

    @Test
    void joinWithUnknownCodeReturnsBadRequest() throws Exception {
        when(joinHousehold.join(any())).thenThrow(new UnknownInviteCodeException());

        mockMvc.perform(post("/api/household/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"inviteCode": "AAAA-BBBB"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("No household has this invite code."));
    }

    @Test
    void joinWhenAlreadyInAHouseholdReturnsConflict() throws Exception {
        when(joinHousehold.join(any())).thenThrow(new AlreadyInHouseholdException());

        mockMvc.perform(post("/api/household/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"inviteCode": "AAAA-BBBB"}
                    """))
            .andExpect(status().isConflict());
    }
}
