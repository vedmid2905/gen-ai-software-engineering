package banking.pipeline;

import banking.pipeline.agent.ComplianceSettlementAgent;
import banking.pipeline.agent.FraudDetectorAgent;
import banking.pipeline.agent.ReportingAgent;
import banking.pipeline.agent.TransactionValidatorAgent;
import banking.pipeline.domain.FraudAssessment;
import banking.pipeline.domain.PipelineSummary;
import banking.pipeline.domain.ProcessingMessage;
import banking.pipeline.domain.ProcessingResult;
import banking.pipeline.domain.TransactionRecord;
import banking.pipeline.io.SharedDirectoryService;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Sets up the shared/ directories, loads sample-transactions.json, and runs
 * every transaction through the validator -> fraud detector -> compliance &
 * settlement -> reporting chain. Each stage's output is persisted as JSON so
 * the run is fully inspectable and repeatable without leftover state.
 */
public class PipelineOrchestrator {

    private final SharedDirectoryService sharedDirectoryService;
    private final TransactionValidatorAgent validatorAgent = new TransactionValidatorAgent();
    private final FraudDetectorAgent fraudDetectorAgent = new FraudDetectorAgent();
    private final ComplianceSettlementAgent complianceSettlementAgent = new ComplianceSettlementAgent();
    private final ReportingAgent reportingAgent;

    public PipelineOrchestrator(Path sharedBaseDir) {
        this.sharedDirectoryService = new SharedDirectoryService(sharedBaseDir);
        this.reportingAgent = new ReportingAgent(sharedDirectoryService);
    }

    public PipelineSummary run(Path inputFile) throws IOException {
        sharedDirectoryService.resetDirectories();

        List<TransactionRecord> transactions = sharedDirectoryService.mapper()
                .readValue(inputFile.toFile(), new TypeReference<List<TransactionRecord>>() {
                });

        for (TransactionRecord transaction : transactions) {
            ProcessingMessage inputMessage = ProcessingMessage.of(
                    "integrator", "transaction_validator", "transaction", transaction);
            sharedDirectoryService.write(sharedDirectoryService.inputDir(),
                    transaction.transactionId() + ".json", inputMessage);
        }

        List<ProcessingResult> finalResults = new ArrayList<>();
        for (TransactionRecord transaction : transactions) {
            ProcessingResult validation = validatorAgent.validateTransaction(transaction);
            sharedDirectoryService.write(sharedDirectoryService.processingDir(),
                    transaction.transactionId() + ".json", validation);

            ProcessingResult finalResult;
            if ("rejected".equals(validation.status())) {
                finalResult = validation;
            } else {
                FraudAssessment assessment = fraudDetectorAgent.scoreTransaction(transaction);
                sharedDirectoryService.write(sharedDirectoryService.outputDir(),
                        transaction.transactionId() + ".json", assessment);
                finalResult = complianceSettlementAgent.evaluateSettlement(transaction, assessment);
            }

            sharedDirectoryService.write(sharedDirectoryService.resultsDir(),
                    transaction.transactionId() + ".json", finalResult);
            finalResults.add(finalResult);
        }

        return reportingAgent.writeSummary(finalResults);
    }

    /**
     * Resolves sample-transactions.json relative to the current working directory,
     * falling back to the parent directory (the layout used when running via Maven
     * from the project root, where the file lives one level up).
     */
    public static Path resolveDefaultInputFile() {
        Path here = Path.of("sample-transactions.json");
        if (Files.isRegularFile(here)) {
            return here;
        }
        return Path.of("..", "sample-transactions.json");
    }
}
