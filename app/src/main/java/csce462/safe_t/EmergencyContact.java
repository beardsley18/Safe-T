package csce462.safe_t;

/**
 * Created by Sarah on 3/31/2017.
 */

public class EmergencyContact {

    private String name;
    private String phone;
    private String email;

    public EmergencyContact(String n, String p, String e){
        name = n;
        phone = p;
        email = e;
    }

    @Override
    public String toString(){
        return name + '\n' + phone + '\n' + email;
    }
}
