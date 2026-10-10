package projet.main;

import src.annotation.Controller;
import src.annotation.JsonObjet;
import src.annotation.JsonResponse;

@Controller(urlBase = "/test")
public class TestController {

    @JsonResponse
    public String hello() {
        return "Hello World";
    }

    @JsonObjet 
    public Personne personne(){
        return new Personne("Rakoto","Jean",25);
    }
    
    
}