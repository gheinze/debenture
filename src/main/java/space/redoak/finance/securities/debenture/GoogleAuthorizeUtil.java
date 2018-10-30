package space.redoak.finance.securities.debenture;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.SheetsScopes;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Copied from: https://www.baeldung.com/google-sheets-java-client
 */
public class GoogleAuthorizeUtil {

   private static final String GOOGLE_SHEET_KEY = System.getenv("GOOGLE_SHEET_KEY");
   private static final File DATA_DIRECTORY = new File("/home/gheinze/CODE/debenture/data/googleCredential");


    public static Credential authorize() throws IOException, GeneralSecurityException {

//        InputStream in = GoogleAuthorizeUtil.class.getResourceAsStream("/google-sheets-debenture-client-secret.json");
//        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JacksonFactory.getDefaultInstance(), new InputStreamReader(in));

        InputStream is = new ByteArrayInputStream( GOOGLE_SHEET_KEY.getBytes( Charset.defaultCharset() ) );
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JacksonFactory.getDefaultInstance(), new InputStreamReader(is));

        List<String> scopes = Arrays.asList(SheetsScopes.SPREADSHEETS);

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                clientSecrets,
                scopes
        ).setDataStoreFactory(new com.google.api.client.util.store.FileDataStoreFactory(DATA_DIRECTORY))
                //MemoryDataStoreFactory()
                .setAccessType("offline")
                .build()
                ;

        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");

        return credential;
    }

}
