package za.co.sb.bounce;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import za.co.sb.bounce.to.UserDetails;

import java.util.ArrayList;
import java.util.List;

public class CognitoHelper {

    private static final String NAME = "name";
    private static final String EMAIL = "email";
    private static final String PHONE_NUMBER = "phone_number";
    private static final String DIAL_CODE = "custom:dial_code";
    private static final String POOL_ID = "eu-west-1_H4alDUjki";

    public UserDetails getUserList(String emailAddress){
        UserDetails userDetails = new UserDetails();
        try {
            CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.builder().build();
            List<String> attributeList = new ArrayList<>();
            attributeList.add(NAME);
            attributeList.add(EMAIL);
            attributeList.add(DIAL_CODE);
            attributeList.add(PHONE_NUMBER);
            ListUsersRequest listUsersRequest = ListUsersRequest.builder()
                    .userPoolId(POOL_ID)
                    .attributesToGet(attributeList)
                    .filter(EMAIL+" =\"" + emailAddress+"\"")
                    .build();

            ListUsersResponse response = cognitoClient.listUsers(listUsersRequest);

            List<UserType> users = response.users();

            for (UserType type : users) {

                for (AttributeType attType : type.attributes()) {
                    if (EMAIL.equals(attType.name()) && emailAddress.equals(attType.value())) {
                        userDetails.setEmail(attType.value());
                    }
                    if (NAME.equals(attType.name())) {
                        userDetails.setName(attType.value());
                    }
                    if (PHONE_NUMBER.equals(attType.name())) {
                        userDetails.setPhoneNumber(attType.value());
                    }
                    if (DIAL_CODE.equals(attType.name())) {
                        userDetails.setDialCode(attType.value());
                    }
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return userDetails;
    }
}
