import java.nio.file.Files;
import java.nio.file.Paths;

public class TestDeleteFile {
    public static void main(String[] args) throws Exception {
        boolean deleted = Files.deleteIfExists(Paths.get("d:/Workspace/Docs/Other/otherA.pdf"));
        System.out.println("Deleted: " + deleted);
    }
}
