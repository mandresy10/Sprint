package projet.main;

import projet.main.model.Employee;
import src.annotation.Controller;
import src.annotation.Url;

@Controller(urlBase = "/employee")
public class EmployeeController {

    @Url("/form")
    public String showForm() {
        return "/views/form-employe.jsp";
    }

    // Le framework va automatiquement instancier Employee et remplir ses champs
    @Url("/save")
    public String saveEmployee(Employee employee) {

        System.out.println("--- DONNÉES REÇUES VIA LE BINDING ---");
        System.out.println("Nom : " + employee.getNom());
        System.out.println("Prénom : " + employee.getPrenom());
        System.out.println("Âge : " + employee.getAge());
        System.out.println("Poste : " + employee.getPoste());

        return "/views/success.jsp";
    }
}