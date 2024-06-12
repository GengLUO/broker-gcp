package be.kuleuven.dsgt4.broker.services;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PBFTService {

    private enum PbftPhase {
        PRE_PREPARE,
        PREPARE,
        COMMIT,
        REPLY
    }

    private static class PbftState {
        PbftPhase phase;
        boolean consensusAchieved;
    }

    // State variables to simulate PBFT consensus
    private final Map<String, PbftState> consensusState = new ConcurrentHashMap<>();

    /**
     * Initiates a PBFT consensus process for the given data.
     *
     * @param data The data to achieve consensus on.
     * @return true if consensus is achieved, false otherwise.
     */
    public boolean initiateConsensus(String data) {
        String requestId = generateRequestId(data);

        // Simulate the PBFT phases
        request(requestId, data);
        prePrepare(requestId, data);
        prepare(requestId);
        return commit(requestId);
    }

    /**
     * Generates a unique request ID based on the data.
     *
     * @param data The data to generate the request ID for.
     * @return The generated request ID.
     */
    private String generateRequestId(String data) {
        return Integer.toString(data.hashCode());
    }

    /**
     * Simulates the Request phase of PBFT.
     *
     * @param requestId The request ID.
     * @param data      The data to achieve consensus on.
     */
    private void request(String requestId, String data) {
        consensusState.put(requestId, new PbftState());
        System.out.println("Request phase for request ID: " + requestId);
    }

    /**
     * Simulates the Pre-Prepare phase of PBFT.
     *
     * @param requestId The request ID.
     * @param data      The data to achieve consensus on.
     */
    private void prePrepare(String requestId, String data) {
        PbftState state = consensusState.get(requestId);
        if (state != null) {
            state.phase = PbftPhase.PRE_PREPARE;
            System.out.println("Pre-Prepare phase for request ID: " + requestId);
        }
    }

    /**
     * Simulates the Prepare phase of PBFT.
     *
     * @param requestId The request ID.
     */
    private void prepare(String requestId) {
        PbftState state = consensusState.get(requestId);
        if (state != null && state.phase == PbftPhase.PRE_PREPARE) {
            state.phase = PbftPhase.PREPARE;
            System.out.println("Prepare phase for request ID: " + requestId);
        }
    }

    /**
     * Simulates the Commit phase of PBFT.
     *
     * @param requestId The request ID.
     * @return true if consensus is achieved, false otherwise.
     */
    private boolean commit(String requestId) {
        PbftState state = consensusState.get(requestId);
        if (state != null && state.phase == PbftPhase.PREPARE) {
            state.phase = PbftPhase.COMMIT;
            state.consensusAchieved = new Random().nextBoolean(); // Simulating consensus decision
            System.out.println("Commit phase for request ID: " + requestId + " - Consensus: " + state.consensusAchieved);
            return state.consensusAchieved;
        }
        return false;
    }

    /**
     * Simulates the Reply phase of PBFT.
     *
     * @param requestId The request ID.
     * @return true if consensus is achieved, false otherwise.
     */
    private boolean reply(String requestId) {
        PbftState state = consensusState.get(requestId);
        if (state != null && state.phase == PbftPhase.COMMIT) {
            state.phase = PbftPhase.REPLY;
            System.out.println("Reply phase for request ID: " + requestId + " - Consensus: " + state.consensusAchieved);
            return state.consensusAchieved;
        }
        return false;
    }
}
