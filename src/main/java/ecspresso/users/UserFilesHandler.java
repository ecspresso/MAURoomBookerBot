package ecspresso.users;


import ecspresso.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.Properties;

public class UserFilesHandler {
    private final HashMap<String, User> users = new HashMap<>();
    private final String path;
    private final Logger logger = new Logger(UserFilesHandler.class);

    public UserFilesHandler(String path) {
        this.path = path;
    }

    public void importUsers() {
        logger.info("Importerar alla användare från {}.", path);
        // Ta bort allt som finns.
        users.clear();
        // Där alls filer finns.
        File folder = new File(path);
        for(File userFile : Objects.requireNonNull(folder.listFiles())) {
            Properties prop = new Properties();
            try (FileInputStream fis = new FileInputStream(userFile)) {
                prop.load(fis);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String username = prop.getProperty("username");
            String password = prop.getProperty("password");
            users.put(username, new User(username, password));
        }
    }

    public boolean contains(String username) {
        return users.containsKey(username);
    }

    public User getUser(String username) {
        return users.get(username);
    }
}
