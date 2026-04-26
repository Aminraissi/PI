package org.example.servicepret.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.servicepret.DTO.FraudAnalysisResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class FraudDetectionService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${fraud.analysis.timeout:120}")
    private int timeoutSeconds;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    private String getScriptDirectory() {
        String workingDir = System.getProperty("user.dir");
        System.out.println("[FRAUD] Working directory: " + workingDir);

        String fraudDir = workingDir + File.separator + "src" + File.separator + "main"
                + File.separator + "java" + File.separator + "org" + File.separator
                + "example" + File.separator + "servicepret" + File.separator + "fraud";

        System.out.println("[FRAUD] Script directory: " + fraudDir);

        File dir = new File(fraudDir);
        if (!dir.exists()) {
            System.err.println("[FRAUD] Répertoire non trouvé: " + fraudDir);
            fraudDir = findFraudDirectory(workingDir);
            System.out.println("[FRAUD] Found fraud directory: " + fraudDir);
        }

        return fraudDir;
    }

    private String findFraudDirectory(String startPath) {
        File root = new File(startPath);
        Queue<File> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            File current = queue.poll();
            if (current.isDirectory()) {
                if (current.getName().equals("fraud")) {
                    return current.getAbsolutePath();
                }
                File[] children = current.listFiles();
                if (children != null) {
                    for (File child : children) {
                        if (child.isDirectory()) {
                            queue.add(child);
                        }
                    }
                }
            }
        }
        return "src/main/java/org/example/servicepret/fraud";
    }

    private String getPythonExecutable() {
        String os = System.getProperty("os.name").toLowerCase();
        String fraudDir = getScriptDirectory();

        String venvPythonPath;
        if (os.contains("win")) {
            venvPythonPath = Paths.get(fraudDir, "venv", "Scripts", "python.exe").toString();
        } else {
            venvPythonPath = Paths.get(fraudDir, "venv", "bin", "python").toString();
        }

        File venvPython = new File(venvPythonPath);
        if (venvPython.exists()) {
            System.out.println("[FRAUD] Using venv Python: " + venvPythonPath);
            return venvPythonPath;
        }

        String systemPython = os.contains("win") ? "python" : "python3";
        System.out.println("[FRAUD] venv not found, using system Python: " + systemPython);
        return systemPython;
    }

    public FraudAnalysisResult analyzeDocuments(
            List<byte[]> decryptedFiles,
            List<String> fileNames,
            Long agriculteurId,
            Long demandeId) throws Exception {

        Path tempDir = Files.createTempDirectory("fraud_analysis_" + demandeId);

        try {
            Map<String, String> filePathMap = new HashMap<>();
            for (int i = 0; i < decryptedFiles.size(); i++) {
                String filename = fileNames.get(i);
                Path tempFile = tempDir.resolve(filename);
                Files.write(tempFile, decryptedFiles.get(i));
                filePathMap.put(filename, tempFile.toString());
            }

            String result = callPythonAnalyzer(filePathMap, agriculteurId, demandeId);
            return parseAnalysisResult(result);

        } finally {
            deleteDirectory(tempDir);
        }
    }

    private String callPythonAnalyzer(Map<String, String> filePathMap, Long agriculteurId, Long demandeId)
            throws Exception {

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("files", filePathMap);
        inputData.put("agriculteur_id", agriculteurId);
        inputData.put("demande_id", demandeId);

        String inputJson = objectMapper.writeValueAsString(inputData);
        Path inputFile = Files.createTempFile("fraud_input_", ".json");
        Files.write(inputFile, inputJson.getBytes());

        try {
            String scriptPath = Paths.get(getScriptDirectory(), "cli.py").toString();
            System.out.println("[FRAUD] Script path: " + scriptPath);

            File scriptFile = new File(scriptPath);
            if (!scriptFile.exists()) {
                throw new RuntimeException("Script Python non trouvé: " + scriptPath);
            }

            String pythonCmd = getPythonExecutable();

            ProcessBuilder pb = new ProcessBuilder(
                    pythonCmd, scriptPath, inputFile.toString()
            );

            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.environment().put("PYTHONUTF8", "1");

            if (groqApiKey != null && !groqApiKey.isEmpty()) {
                pb.environment().put("GROQ_API_KEY", groqApiKey);
                System.out.println("[FRAUD] GROQ_API_KEY configurée depuis application.properties");
            } else {
                String envKey = System.getenv("GROQ_API_KEY");
                if (envKey != null && !envKey.isEmpty()) {
                    pb.environment().put("GROQ_API_KEY", envKey);
                    System.out.println("[FRAUD] GROQ_API_KEY trouvée dans les variables système");
                } else {
                    System.err.println("[FRAUD] WARNING: GROQ_API_KEY non définie");
                }
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    System.out.println("[PYTHON] " + line);
                }
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Fraud analysis timeout after " + timeoutSeconds + " seconds");
            }

            if (process.exitValue() != 0) {
                throw new RuntimeException("Python script error: " + output);
            }

            return output.toString();

        } finally {
            Files.deleteIfExists(inputFile);
        }
    }

    /**
     * Extrait le JSON de la sortie Python (les logs sont ignorés)
     */
    private String extractJsonFromOutput(String output) {
        int start = output.indexOf('{');
        int end = output.lastIndexOf('}');

        if (start != -1 && end != -1 && end > start) {
            String jsonPart = output.substring(start, end + 1);
            System.out.println("[FRAUD] Extracted JSON length: " + jsonPart.length());
            return jsonPart;
        }
        return output;
    }

    private FraudAnalysisResult parseAnalysisResult(String json) throws Exception {
        // 🔥 Extraire le JSON des logs
        String cleanJson = extractJsonFromOutput(json);

        // Log pour déboguer
        System.out.println("[FRAUD] Parsing JSON (first 200 chars): " +
                cleanJson.substring(0, Math.min(200, cleanJson.length())));

        JsonNode root = objectMapper.readTree(cleanJson);

        FraudAnalysisResult result = new FraudAnalysisResult();
        result.setGlobalRisk(root.path("global_risk").asText());
        result.setGlobalScore(root.path("global_score").asInt());
        result.setFraudConfirmed(root.path("fraud_confirmed").asBoolean());
        result.setRecommendation(root.path("recommendation").asText());
        result.setRecommendationJustification(root.path("recommendation_justification").asText());
        result.setDossierNarrative(root.path("dossier_narrative").asText());

        List<FraudAnalysisResult.SuspiciousField> suspiciousFields = new ArrayList<>();
        root.path("all_suspicious_fields").forEach(field -> {
            FraudAnalysisResult.SuspiciousField sf = new FraudAnalysisResult.SuspiciousField();
            sf.setDocument(field.path("document").asText());
            sf.setFieldName(field.path("field_name").asText());
            sf.setSuspiciousValue(field.path("suspicious_value").asText());
            sf.setReason(field.path("reason").asText());
            sf.setSeverity(field.path("severity").asText());
            suspiciousFields.add(sf);
        });
        result.setAllSuspiciousFields(suspiciousFields);

        List<String> inconsistencies = new ArrayList<>();
        root.path("cross_document_inconsistencies").forEach(inc ->
                inconsistencies.add(inc.asText()));
        result.setCrossDocumentInconsistencies(inconsistencies);

        List<String> criticalDocs = new ArrayList<>();
        root.path("critical_documents").forEach(doc ->
                criticalDocs.add(doc.asText()));
        result.setCriticalDocuments(criticalDocs);

        List<FraudAnalysisResult.DocumentAnalysis> documents = new ArrayList<>();
        root.path("documents").forEach(doc -> {
            FraudAnalysisResult.DocumentAnalysis da = new FraudAnalysisResult.DocumentAnalysis();
            da.setFilename(doc.path("filename").asText());
            da.setDocType(doc.path("doc_type").asText());
            da.setRiskLevel(doc.path("risk_level").asText());
            da.setRiskScore(doc.path("risk_score").asInt());
            da.setAiGeneratedProbability(doc.path("ai_generated_probability").asInt());
            da.setDocumentNarrative(doc.path("document_narrative").asText());

            JsonNode meta = doc.path("metadata");
            FraudAnalysisResult.Metadata metadata = new FraudAnalysisResult.Metadata();
            metadata.setCreator(meta.path("creator").asText());
            metadata.setProducer(meta.path("producer").asText());
            metadata.setMetadataRisk(meta.path("metadata_risk").asText());
            da.setMetadata(metadata);

            List<String> signals = new ArrayList<>();
            doc.path("fraud_signals").forEach(signal ->
                    signals.add(signal.path("detail").asText()));
            da.setFraudSignals(signals);

            documents.add(da);
        });
        result.setDocuments(documents);

        JsonNode stats = root.path("stats");
        result.setTotalDocuments(stats.path("total_documents").asInt());
        result.setHighRiskDocuments(stats.path("high_risk_documents").asInt());
        result.setMediumRiskDocuments(stats.path("medium_risk_documents").asInt());
        result.setLowRiskDocuments(stats.path("low_risk_documents").asInt());

        return result;
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            System.err.println("Failed to delete: " + path);
                        }
                    });
        }
    }
}