package projet.main;

import src.annotation.Controller;
import src.annotation.Param;
import src.annotation.Url;

@Controller(urlBase = "/employee")
public class EmployeeController {

    // Affiche le formulaire JSP
    @Url("/form")
    public String showForm() {
        return "/views/form-employe.jsp";
    }

    // Recupere et identifie les valeurs saisies dans le formulaire
    @Url("/save")
    public String saveEmployee(
            @Param("nom") String nom,
            @Param("prenom") String prenom,
            @Param("age") int age,
            @Param("poste") String poste) {

        System.out.println("--- DONNEES REÇUES DEPUIS LE FORMULAIRE ---");
        System.out.println("Nom : " + nom);
        System.out.println("Prénom : " + prenom);
        System.out.println("Âge : " + age);
        System.out.println("Poste : " + poste);

        return "/views/success.jsp";
    }
}
