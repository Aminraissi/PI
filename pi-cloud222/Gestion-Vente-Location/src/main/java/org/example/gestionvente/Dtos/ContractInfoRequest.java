package org.example.gestionvente.Dtos;


public class ContractInfoRequest {

    private String signatureAgriculteur;
    private String clausesContrat;

    public String getSignatureAgriculteur() {
        return signatureAgriculteur;
    }

    public void setSignatureAgriculteur(String signatureAgriculteur) {
        this.signatureAgriculteur = signatureAgriculteur;
    }

    public String getClausesContrat() {
        return clausesContrat;
    }

    public void setClausesContrat(String clausesContrat) {
        this.clausesContrat = clausesContrat;
    }
}