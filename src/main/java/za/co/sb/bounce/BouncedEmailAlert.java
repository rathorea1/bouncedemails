package za.co.sb.bounce;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import za.co.sb.bounce.to.UserDetails;


public class BouncedEmailAlert implements RequestHandler<SNSEvent, Object> {

    private final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());
    private static final String TABLE_NAME = "bounced_emails";
    private static final String NAME = "name";
    private static final String EMAIL = "email";
    private static final String DIAL_CODE = "dial_code";
    private static final String PHONE_NUMBER = "phone_number";
    private static final String FAILURE_TIME = "failure_time";
    private static final String BOUNCE_TYPE = "bounce_type";
    private static final String BOUNCE_SUB_TYPE = "bounce_sub_type";
    private static final String REASON = "reason";

    public Object handleRequest(SNSEvent request, Context context){

        try {
            LambdaLogger logger = context.getLogger();

            List<SNSEvent.SNSRecord> snsRecordList = request.getRecords();
            if (snsRecordList != null) {
                SNSEvent.SNS recordSNS = null;
                for (SNSEvent.SNSRecord snsRecord : snsRecordList) {
                    recordSNS = snsRecord.getSNS();
                    JSONObject json = new JSONObject(recordSNS.getMessage());
                    JSONArray arr = json.getJSONObject("mail").getJSONArray("destination");
                    String emailAddress = null;
                    for (int i = 0; i < arr.length(); i++) {
                        emailAddress = arr.get(0).toString();
                    }
                    String timeStamp = json.getJSONObject("mail").getString("timestamp");
                    String bounceType = json.getJSONObject("bounce").getString("bounceType");
                    String bounceSubType = json.getJSONObject("bounce").getString("bounceSubType");

                    String reason = "";
                    if ("Permanent".equals(bounceType)){
                        JSONArray bounceReasonArr = json.getJSONObject("bounce").getJSONArray("bouncedRecipients");
                        for (int i = 0; i < bounceReasonArr.length(); i++) {
                            JSONObject temp = bounceReasonArr.getJSONObject(0);
                            reason = temp.getString("diagnosticCode");
                        }
                    }

                    UserDetails userDetails =new CognitoHelper().getUserList(emailAddress);
                    logger.log(userDetails.getEmail());
                    logger.log(userDetails.getDialCode());
                    logger.log(userDetails.getName());
                    logger.log(userDetails.getPhoneNumber());

                    Table table = dynamoDB.getTable(TABLE_NAME);

                    Item item = new Item()
                            .withPrimaryKey(EMAIL, userDetails.getEmail())
                            .withString(FAILURE_TIME, timeStamp)
                            .withString(NAME, userDetails.getName())
                            .withString(DIAL_CODE, userDetails.getDialCode())
                            .withString(PHONE_NUMBER, userDetails.getPhoneNumber())
                            .withString(BOUNCE_TYPE, bounceType)
                            .withString(BOUNCE_SUB_TYPE, bounceSubType)
                            .withString(REASON, reason);

                    table.putItem(item);
                    logger.log("Insert successful");

                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}