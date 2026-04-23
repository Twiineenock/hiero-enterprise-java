package org.hiero.microprofile.sample;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hiero.base.AccountClient;
import org.hiero.base.data.Account;
import org.hiero.base.data.ContractLog;
import org.hiero.base.data.Page;
import org.hiero.base.mirrornode.ContractLogRepository;

@Path("/")
public class HieroEndpoint {

  private final AccountClient client;
  private final ContractLogRepository contractLogRepository;

  @Inject
  public HieroEndpoint(
      final AccountClient client, final ContractLogRepository contractLogRepository) {
    this.client = client;
    this.contractLogRepository = contractLogRepository;
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String createAccount() {
    try {
      final Account account = client.createAccount();
      return "Account created!";
    } catch (final Exception e) {
      throw new RuntimeException("Error in Hedera call", e);
    }
  }

  @GET
  @Path("/contract/{contractId}/logs")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getLogs(@PathParam("contractId") String contractId) {
    try {
      Page<ContractLog> logsPage = contractLogRepository.findByContractId(contractId);

      Map<String, Object> response = new LinkedHashMap<>();
      response.put("status", "SUCCESS");
      response.put("contractId", contractId);
      response.put("logs", logsPage.getData());
      return Response.ok(response).build();
    } catch (final Exception e) {
      Map<String, Object> response = new LinkedHashMap<>();
      response.put("status", "ERROR");
      response.put("error", e.getMessage());
      return Response.serverError().entity(response).build();
    }
  }
}
