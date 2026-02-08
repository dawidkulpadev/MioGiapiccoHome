package pl.dawidkulpa.miogiapiccohome.API.requests;

public class ActivationRequest {
    String login;
    String pass;
    String pin;
    public ActivationRequest(String l, String p, String pi) { login = l; pass = p; pin = pi; }
}
