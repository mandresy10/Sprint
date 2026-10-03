package projet.main;

import src.annotation.Controller;
import src.annotation.JsonResponse;

@Controller(urlBase = "/test")
public class TestController {
    
    @JsonResponse 
    public String Hello() {
        return "Hello World";
    }
}