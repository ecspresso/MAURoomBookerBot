package ecspresso.email;

import ecspresso.Logger;
import ecspresso.mau.Building;

import jakarta.mail.Address;

import java.util.ArrayList;
import java.util.Arrays;

public record EmailMessage(Address[] from, String[] subject) {
    private static final Logger logger = new Logger(EmailMessage.class);
    public boolean validSubject(String reason) {
        logger.info("Kontrollerar om rubriken {} är giltig. (Anledning: {})", String.join(" ", subject), reason);

        boolean valid =  subject.length == 3 || subject.length == 4; // Kontrollera om antalet ord i rubriken är 2 eller 3.

        if(valid) {
            for(int i = 2; i < subject.length && valid; i++) { // Kontrollera om plats 2 och 3 (0 indexerat) är siffror.
                try {
                    int time = Integer.parseInt(subject[i]); // Förvandla till siffra.
                    if(time < 1 || time > 5) throw new NumberFormatException(); // Kasta fel om man försöker boka annat än 1-5.
                } catch(NumberFormatException e) {
                    valid = false; // Returnera false om vi misslyckades med förvandlingen..
                }
            }
        }

        return valid;
    }

    public String getUserName() {
        return validSubject("Användnamn") ? subject[0] : null;
    }

    public Building getBuilding() { // Hämta vilken byggnad man vill boka i.
        String buildingName = validSubject("Byggnad") ? subject[1] : null; // Kontrollera att det är korrekt formaterat.
        if(buildingName != null) {
            return Building.convertString(buildingName);
        } else {
            return null;
        }
    }

    public ArrayList<Integer> getTimes() {
        int length = validSubject("Tider") ? subject.length - 2 : 0; // Sätt hur många rum som ska bokas, 0 om felaktig rubrik.
        ArrayList<Integer> times = new ArrayList<>();

        for(int i = 0; i < length; i++) { // Hämta alla rum, börja på plats 2 i rubriken (0 -> 1 -> 2 -> (3)).
            times.add(Integer.parseInt(subject[i + 2])); // Spara i times.
        }

        return times;
    }

    @Override
    public String toString() {
        return "EmailMessage{" + "from=" + Arrays.toString(from) + ", subject=" + Arrays.toString(subject) + '}';
    }
}
