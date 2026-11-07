package hongik21.fit_a_pet.security;

import hongik21.fit_a_pet.IntegrationSupport;
import hongik21.fit_a_pet.accounts.entity.*;
import hongik21.fit_a_pet.accounts.repository.MemberRepository;
import hongik21.fit_a_pet.accounts.service.TokenService;
import hongik21.fit_a_pet.pets.entity.*;
import hongik21.fit_a_pet.pets.repository.PetRepository;
import hongik21.fit_a_pet.posts.domain.*;
import hongik21.fit_a_pet.posts.repository.PostRepository;
import hongik21.fit_a_pet.posts.service.PostService;
import hongik21.fit_a_pet.trails.domain.WalkRecord;
import hongik21.fit_a_pet.trails.repository.WalkRecordRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

class OwnershipAndPagingIntegrationTest extends IntegrationSupport {
    @Autowired MemberRepository members;
    @Autowired PetRepository pets;
    @Autowired WalkRecordRepository walks;
    @Autowired PostRepository posts;
    @Autowired PostService postService;
    @Autowired TokenService tokens;
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate sql;
    @Autowired EntityManagerFactory emf;
    Member a,b; Pet petB; String access;
    @BeforeEach void setup() {
        a=member();b=member();
        petB=pets.save(Pet.builder().member(b).name("B-pet").age(2).gender(PetGenderType.values()[0])
                .type(PetType.values()[0]).createdAt(LocalDateTime.now()).build());
        access=tokens.login(a).accessToken();
    }
    Member member(){return members.save(Member.builder().name("synthetic").email(UUID.randomUUID()+"@test.com")
            .nickname("user").password("not-used").memberType(MemberType.GENERAL).createdAt(LocalDateTime.now()).build());}
    WalkRecord record(Member m,String memo) {
        return walks.save(WalkRecord.builder().memberId(m).walkDate(LocalDate.of(2026,11,1))
                .walkStart(LocalTime.NOON).walkEnd(LocalTime.of(13,0)).formattedDuration("01:00:00")
                .memo(memo).address("synthetic").build());
    }
    @Test void monthlyRecordsNeverIncludeOtherMemberAndDirectAccessCannotMutateThem() throws Exception {
        WalkRecord own=record(a,"own"), other=record(b,"private-B");
        mvc.perform(get("/trails?year=2026&month=11").header("Authorization","Bearer "+access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data",hasSize(1)))
                .andExpect(jsonPath("$.data[0].recordId").value(own.getRecordId()));
        for(String method:List.of("GET","PUT","DELETE")) {
            var request=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                    org.springframework.http.HttpMethod.valueOf(method),"/trails/"+other.getRecordId())
                    .header("Authorization","Bearer "+access).contentType("application/json").content("{\"memo\":\"hacked\",\"rating\":1}");
            mvc.perform(request).andExpect(status().is4xxClientError());
        }
        assertEquals("private-B",walks.findById(other.getRecordId()).orElseThrow().getMemo());
    }
    @Test void cannotAttachOtherMembersPetToWalkOrPetPostOrUpdatePet() throws Exception {
        mvc.perform(post("/trails").header("Authorization","Bearer "+access).contentType("application/json")
                .content("{\"petId\":"+petB.getId()+",\"walkDate\":\"2026-11-01\",\"walkStart\":\"12:00\",\"walkEnd\":\"13:00\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/mypage/pet/"+petB.getId()).header("Authorization","Bearer "+access)
                .contentType("application/json").content("{\"name\":\"hacked\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/petposts").header("Authorization","Bearer "+access).contentType("application/json")
                .content("{\"petId\":"+petB.getId()+",\"petPostTitle\":\"bad link\"}"))
                .andExpect(status().isNotFound());
        assertEquals("B-pet",pets.findById(petB.getId()).orElseThrow().getName());
    }
    @Test void otherMembersPostCannotBeChangedOrDeleted() throws Exception {
        Post p=posts.save(FreePost.builder().member(b).postTitle("B title").postContent("private edit")
                .postDate(LocalDateTime.now()).build());
        mvc.perform(put("/posts/"+p.getPostId()).header("Authorization","Bearer "+access)
                .contentType("application/json").content("{\"postTitle\":\"hacked\",\"postContent\":\"hacked\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/posts/"+p.getPostId()).header("Authorization","Bearer "+access))
                .andExpect(status().isNotFound());
        assertEquals("B title",posts.findById(p.getPostId()).orElseThrow().getPostTitle());
    }
    @Test void petPostModificationAndDevelopmentBypassRoutesAreBlocked() throws Exception {
        sql.update("insert into pet_post(member_id,pet_id,pet_post_title) values(?,?,?)",b.getId(),petB.getId(),"B-post");
        Long id=sql.queryForObject("select max(pet_post_id) from pet_post",Long.class);
        mvc.perform(put("/petposts/"+id).header("Authorization","Bearer "+access).contentType("application/json")
                .content("{\"petPostTitle\":\"hacked\"}")).andExpect(status().isNotFound());
        mvc.perform(delete("/petposts/"+id).header("Authorization","Bearer "+access)).andExpect(status().isNotFound());
        assertEquals("B-post",sql.queryForObject("select pet_post_title from pet_post where pet_post_id=?",String.class,id));
        mvc.perform(post("/api/test/pet").header("Authorization","Bearer "+access).contentType("application/json")
                .content("{}")).andExpect(status().isForbidden());
    }
    @Test void cursorHandlesEqualDatesWithoutDuplicatesAndQueryIsBounded() {
        posts.deleteAll();
        LocalDateTime date=LocalDateTime.of(2026,11,1,12,0);
        Set<Long> expected=new HashSet<>();
        for(int i=0;i<47;i++) expected.add(posts.save(FreePost.builder().member(a).postTitle("post-"+i)
                .postContent("body").postDate(date).build()).getPostId());
        posts.save(InfoPost.builder().member(b).postTitle("exclude").postDate(date).build());
        var stats=emf.unwrap(SessionFactory.class).getStatistics(); stats.clear();
        Set<Long> actual=new HashSet<>();
        LocalDateTime cursorDate=null;Long cursorId=null;int pageCount=0;
        do {
            var page=postService.listPost(PostCategoryType.FREE,cursorDate,cursorId,20);
            assertTrue(page.items().size()<=20);
            for(var item:page.items()){assertTrue(actual.add(item.getPostId()));assertEquals(PostCategoryType.FREE,item.getPostCategory());}
            pageCount++;
            if(!page.hasNext())break;
            cursorDate=page.nextDate();cursorId=page.nextId();
        }while(pageCount<10);
        assertEquals(expected,actual);assertEquals(3,pageCount);
        assertEquals(6,stats.getPrepareStatementCount()); // two bounded SQLs per page, no count or N+1
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                ()->postService.listPost(null,date,null,20));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                ()->postService.listPost(null,null,null,21));
    }
}
