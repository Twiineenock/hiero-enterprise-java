package org.hiero.spring.sample;

import com.hedera.hashgraph.sdk.ContractId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.hiero.base.SmartContractClient;
import org.hiero.base.data.ContractCallResult;
import org.hiero.base.mirrornode.ContractLogRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SmartContractEndpoint {

  private final SmartContractClient smartContractClient;
  private final ContractLogRepository contractLogRepository;
  private ContractId lastDeployedContractId;

  public SmartContractEndpoint(
      final SmartContractClient smartContractClient,
      final ContractLogRepository contractLogRepository) {
    this.smartContractClient =
        Objects.requireNonNull(smartContractClient, "smartContractClient must not be null");
    this.contractLogRepository =
        Objects.requireNonNull(contractLogRepository, "contractLogRepository must not be null");
  }

  /**
   * Deploy a simple counter smart contract. Solidity source:
   *
   * <pre>
   * pragma solidity ^0.8.0;
   * contract Counter {
   *     uint256 public count;
   *     constructor() { count = 0; }
   *     function increment() public { count += 1; }
   *     function get_count() public view returns (uint256) { return count; }
   * }
   * </pre>
   */
  @PostMapping("/contract/deploy")
  public Map<String, String> deployContract() {
    try {
      // Compiled bytecode for the Counter contract (solc 0.8.34, evmVersion: paris)
      final String bytecodeHex =
          "6080604052348015600f57600080fd5b5060008081905550610183806100266000396000f3fe608060405234801561001057600080fd5b50600436106100415760003560e01c806306661abd14610046578063d09de08a14610064578063e7278e7f1461006e575b600080fd5b61004e61008c565b60405161005b91906100cf565b60405180910390f35b61006c610092565b005b6100766100ad565b60405161008391906100cf565b60405180910390f35b60005481565b60016000808282546100a49190610119565b92505081905550565b60008054905090565b6000819050919050565b6100c9816100b6565b82525050565b60006020820190506100e460008301846100c0565b92915050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052601160045260246000fd5b6000610124826100b6565b915061012f836100b6565b9250828201905080821115610147576101466100ea565b5b9291505056fea2646970667358221220f83ab71852f3d6871fe74a4cbf14bf72e49553d220fe71872e2ab4dd5060168264736f6c63430008220033";

      final byte[] bytecode = hexStringToByteArray(bytecodeHex);
      final ContractId contractId = smartContractClient.createContract(bytecode);

      lastDeployedContractId = contractId;

      Map<String, String> result = new LinkedHashMap<>();
      result.put("status", "SUCCESS");
      result.put("contractId", contractId.toString());
      result.put("description", "Counter contract deployed! Use the endpoints below.");
      result.put("getCount", "GET /contract/" + contractId + "/count");
      result.put("increment", "POST /contract/" + contractId + "/increment");
      return result;
    } catch (final Exception e) {
      Map<String, String> result = new LinkedHashMap<>();
      result.put("status", "ERROR");
      result.put("error", e.getMessage());
      Throwable cause = e.getCause();
      int i = 0;
      while (cause != null) {
        result.put("cause_" + i, cause.getClass().getSimpleName() + ": " + cause.getMessage());
        cause = cause.getCause();
        i++;
      }
      return result;
    }
  }

  /** Read the current count from the counter contract. */
  @GetMapping("/contract/{contractId}/count")
  public Map<String, Object> getCount(@PathVariable("contractId") String contractId) {
    try {
      final ContractCallResult result =
          smartContractClient.callContractFunction(contractId, "get_count");

      Map<String, Object> response = new LinkedHashMap<>();
      response.put("status", "SUCCESS");
      response.put("contractId", contractId);
      response.put("count", result.getInt64(0));
      return response;
    } catch (final Exception e) {
      Map<String, Object> response = new LinkedHashMap<>();
      response.put("status", "ERROR");
      response.put("error", e.getMessage());
      return response;
    }
  }

  /** Increment the counter on the contract. */
  @PostMapping("/contract/{contractId}/increment")
  public Map<String, String> increment(@PathVariable("contractId") String contractId) {
    try {
      smartContractClient.callContractFunction(contractId, "increment");

      Map<String, String> result = new LinkedHashMap<>();
      result.put("status", "SUCCESS");
      result.put("contractId", contractId);
      result.put("action", "Counter incremented! GET /contract/" + contractId + "/count to see.");
      return result;
    } catch (final Exception e) {
      Map<String, String> result = new LinkedHashMap<>();
      result.put("status", "ERROR");
      result.put("error", e.getMessage());
      return result;
    }
  }

  /** Quick access to the last deployed contract count. */
  @GetMapping("/contract/latest")
  public Map<String, Object> getLatest() {
    if (lastDeployedContractId == null) {
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("status", "NO_CONTRACT");
      result.put("hint", "POST /contract/deploy first");
      return result;
    }
    return getCount(lastDeployedContractId.toString());
  }

  private static byte[] hexStringToByteArray(String hex) {
    int len = hex.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] =
          (byte)
              ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
    }
    return data;
  }

  /** Get logs for the contract. */
  @GetMapping("/contract/{contractId}/logs")
  public Map<String, Object> getLogs(@PathVariable("contractId") String contractId) {
    try {
      org.hiero.base.data.Page<org.hiero.base.data.ContractLog> logsPage =
          contractLogRepository.findByContractId(contractId);

      Map<String, Object> response = new LinkedHashMap<>();
      response.put("status", "SUCCESS");
      response.put("contractId", contractId);
      response.put("logs", logsPage.getData());
      return response;
    } catch (final Exception e) {
      Map<String, Object> response = new LinkedHashMap<>();
      response.put("status", "ERROR");
      response.put("error", e.getMessage());
      return response;
    }
  }
}
