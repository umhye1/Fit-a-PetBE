package hongik21.fit_a_pet.trails.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WalkRecordController {

    @GetMapping("/doTest")
    public String doTest(){
        System.out.println("test");
        return "테스트 성공";
    }
}