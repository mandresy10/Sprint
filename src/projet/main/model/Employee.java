package projet.main.model;

public class Employee {
    private String nom;
    private String prenom;
    private int age;
    private String poste;

    // Getters et Setters
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getPoste() { return poste; }
    public void setPoste(String poste) { this.poste = poste; }

    @Override
    public String toString() {
        return "Employee{nom='" + nom + "', prenom='" + prenom + "', age=" + age + ", poste='" + poste + "'}";
    }
}