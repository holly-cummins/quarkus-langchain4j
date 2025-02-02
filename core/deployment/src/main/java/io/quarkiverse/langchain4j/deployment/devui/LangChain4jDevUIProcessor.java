package io.quarkiverse.langchain4j.deployment.devui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.quarkiverse.langchain4j.deployment.DeclarativeAiServiceBuildItem;
import io.quarkiverse.langchain4j.deployment.EmbeddingModelBuildItem;
import io.quarkiverse.langchain4j.deployment.EmbeddingStoreBuildItem;
import io.quarkiverse.langchain4j.deployment.ToolsMetadataBuildItem;
import io.quarkiverse.langchain4j.deployment.items.ChatModelProviderCandidateBuildItem;
import io.quarkiverse.langchain4j.deployment.items.SelectedChatModelProviderBuildItem;
import io.quarkiverse.langchain4j.runtime.devui.ChatJsonRPCService;
import io.quarkiverse.langchain4j.runtime.devui.EmbeddingStoreJsonRPCService;
import io.quarkiverse.langchain4j.runtime.tool.ToolMethodCreateInfo;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

public class LangChain4jDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    CardPageBuildItem cardPage(List<DeclarativeAiServiceBuildItem> aiServices,
            ToolsMetadataBuildItem toolsMetadataBuildItem,
            List<EmbeddingModelBuildItem> embeddingModelBuildItem,
            List<EmbeddingStoreBuildItem> embeddingStoreBuildItem,
            List<SelectedChatModelProviderBuildItem> chatModelProviders) {
        CardPageBuildItem card = new CardPageBuildItem();
        addAiServicesPage(card, aiServices);
        if (toolsMetadataBuildItem != null) {
            addToolsPage(card, toolsMetadataBuildItem);
        }
        // for now, add the embedding store page only if there is a single embedding model and a single embedding store
        // if we allow more in the future, we need a way to specify which ones to use for the page
        if (embeddingModelBuildItem.size() == 1 && embeddingStoreBuildItem.size() == 1) {
            addEmbeddingStorePage(card);
        }
        if (!chatModelProviders.isEmpty()) {
            addChatPage(card);
        }
        return card;
    }

    private void addEmbeddingStorePage(CardPageBuildItem card) {
        card.addPage(Page.webComponentPageBuilder().title("Embedding store")
                .componentLink("qwc-embedding-store.js")
                .icon("font-awesome-solid:database"));
    }

    private void addAiServicesPage(CardPageBuildItem card, List<DeclarativeAiServiceBuildItem> aiServices) {
        List<AiServiceInfo> infos = new ArrayList<>();
        for (DeclarativeAiServiceBuildItem aiService : aiServices) {
            List<String> tools = aiService.getToolDotNames().stream().map(dotName -> dotName.toString()).toList();
            infos.add(new AiServiceInfo(aiService.getServiceClassInfo().name().toString(), tools));
        }

        card.addBuildTimeData("aiservices", infos);
        card.addPage(Page.webComponentPageBuilder().title("AI Services")
                .componentLink("qwc-aiservices.js")
                .staticLabel(String.valueOf(aiServices.size()))
                .icon("font-awesome-solid:robot"));
    }

    private void addToolsPage(CardPageBuildItem card, ToolsMetadataBuildItem metadataBuildItem) {
        List<ToolMethodInfo> infos = new ArrayList<>();
        Map<String, List<ToolMethodCreateInfo>> metadata = metadataBuildItem.getMetadata();
        for (Map.Entry<String, List<ToolMethodCreateInfo>> toolClassEntry : metadata.entrySet()) {
            for (ToolMethodCreateInfo toolMethodCreateInfo : toolClassEntry.getValue()) {
                infos.add(new ToolMethodInfo(toolClassEntry.getKey(),
                        toolMethodCreateInfo.getToolSpecification().name(),
                        toolMethodCreateInfo.getToolSpecification().description()));
            }
        }
        card.addBuildTimeData("tools", infos);
        card.addPage(Page.webComponentPageBuilder().title("Tools")
                .componentLink("qwc-tools.js")
                .staticLabel(String.valueOf(infos.size()))
                .icon("font-awesome-solid:toolbox"));
    }

    private void addChatPage(CardPageBuildItem card) {
        card.addPage(Page.webComponentPageBuilder().title("Chat")
                .componentLink("qwc-chat.js")
                .icon("font-awesome-solid:comments"));
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void jsonRpcProviders(BuildProducer<JsonRPCProvidersBuildItem> producers,
            List<EmbeddingModelBuildItem> embeddingModelBuildItem,
            List<EmbeddingStoreBuildItem> embeddingStoreBuildItem,
            List<ChatModelProviderCandidateBuildItem> chatModelCandidates) {
        if (embeddingModelBuildItem.size() == 1 && embeddingStoreBuildItem.size() == 1) {
            producers.produce(new JsonRPCProvidersBuildItem(EmbeddingStoreJsonRPCService.class));
        }
        if (!chatModelCandidates.isEmpty()) {
            producers.produce(new JsonRPCProvidersBuildItem(ChatJsonRPCService.class));
        }
    }

}
