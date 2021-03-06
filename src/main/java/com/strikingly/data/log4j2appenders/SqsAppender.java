package com.strikingly.data.log4j2appenders;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;

@Plugin(name = "SqsAppender", category = "Core", elementType = "appender", printObject = true)
public class SqsAppender extends AbstractAppender {
    private static AmazonSQS sqsClient;
    private static String sqsUrl = null;

    private SqsAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
    }

    @Override
    public void append(LogEvent event) {
        sqsClient.sendMessage(new SendMessageRequest(sqsUrl, getLayout().toSerializable(event).toString()));
    }

    @PluginFactory
    public static SqsAppender createAppender(@PluginAttribute("name") String name,
                                             @PluginAttribute("QueueName") String queueName,
                                             @PluginElement("Filter") final Filter filter,
                                             @PluginElement("Layout") Layout<? extends Serializable> layout,
                                             @PluginAttribute("ignoreExceptions") boolean ignoreExceptions) {
        if (name == null) {
            System.err.println("no name defined in conf.");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        try {
            String region = System.getProperty("AWS_REGION");
            if (region == null) {
                region = "cn-north-1";
            }

            sqsClient = AmazonSQSClientBuilder.
                    standard()
                    .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                    .withRegion(region)
                    .build();
            GetQueueUrlResult queueUrlResult = sqsClient.getQueueUrl(queueName);
            sqsUrl = queueUrlResult.getQueueUrl();
        } catch (Exception exp) {
            System.err.println(exp);
        }
        return new SqsAppender(name, filter, layout, ignoreExceptions);
    }
}
