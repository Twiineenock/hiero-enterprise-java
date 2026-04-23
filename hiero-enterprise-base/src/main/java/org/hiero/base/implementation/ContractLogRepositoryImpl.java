package org.hiero.base.implementation;

import com.hedera.hashgraph.sdk.ContractId;
import java.util.Objects;
import org.hiero.base.HieroException;
import org.hiero.base.data.ContractLog;
import org.hiero.base.data.Page;
import org.hiero.base.mirrornode.ContractLogRepository;
import org.hiero.base.mirrornode.MirrorNodeClient;
import org.jspecify.annotations.NonNull;

public class ContractLogRepositoryImpl implements ContractLogRepository {

  private final MirrorNodeClient mirrorNodeClient;

  public ContractLogRepositoryImpl(@NonNull final MirrorNodeClient mirrorNodeClient) {
    this.mirrorNodeClient =
        Objects.requireNonNull(mirrorNodeClient, "mirrorNodeClient must not be null");
  }

  @Override
  @NonNull
  public Page<ContractLog> findByContractId(@NonNull ContractId contractId) throws HieroException {
    return mirrorNodeClient.queryContractLogs(contractId);
  }
}
