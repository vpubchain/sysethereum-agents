package org.sysethereum.agents.tool;

import lombok.extern.slf4j.Slf4j;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.store.BlockStoreException;
import org.libdohj.params.AbstractSyscoinParams;
import org.sysethereum.agents.MainLifecycle;
import org.sysethereum.agents.constants.AgentConstants;
import org.sysethereum.agents.constants.SystemProperties;
import org.sysethereum.agents.core.bridge.SuperblockData;
import org.sysethereum.agents.core.bridge.SuperblockFactory;
import org.sysethereum.agents.core.syscoin.SyscoinWrapper;
import org.sysethereum.agents.core.syscoin.Keccak256Hash;
import org.sysethereum.agents.core.bridge.Superblock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.bitcoinj.core.Utils.HEX;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.sysethereum.agents.service.rest.MerkleRootComputer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tool to create a genesis superblock
 * @author Catalina Juarros
 * @author Oscar Guindzberg
 */
@Configuration
@ComponentScan
@Slf4j(topic = "GenesisSuperblockGeneratorMain")
public class GenesisSuperblockGeneratorMain {
    private static final Logger logger = LoggerFactory.getLogger("GenesisSuperblockGeneratorMain");
    private static final String BASE_DIR = "/yourPath/sysethereum-agents";
    private static final String SUB_DIR = "/src/main/java/org/sysethereum/agents/tool";

    public static void main(String[] args) throws Exception {
        // Instantiate the spring context
        AnnotationConfigApplicationContext c = new AnnotationConfigApplicationContext();
        c.registerShutdownHook();

        SystemProperties config = c.getBean(SystemProperties.class);
        logger.info("Running GenesisSuperblockGeneratorMain version: {}-{}", config.projectVersion(), config.projectVersionModifier());

        c.register(SyscoinWrapper.class);
        c.refresh();

        var lifecycle = c.getBean(MainLifecycle.class);
        lifecycle.initialize();

        SyscoinWrapper syscoinWrapper = c.getBean(SyscoinWrapper.class);
        AgentConstants agentConstants = c.getBean(AgentConstants.class);
        SuperblockFactory superblockFactory = c.getBean(SuperblockFactory.class);

        SuperblockData data = getGenesisSuperblock(agentConstants, syscoinWrapper);
        Superblock s = superblockFactory.fromData(data);

        System.out.println(s);
    }

    private static SuperblockData getGenesisSuperblock(AgentConstants agentConstants, SyscoinWrapper syscoinWrapper) throws IOException, BlockStoreException {
        AbstractSyscoinParams params = agentConstants.getSyscoinParams();

        BufferedReader reader = new BufferedReader(
                new FileReader(BASE_DIR + SUB_DIR + "/syscoinmain-2309215-to-2309216"));
        List<Sha256Hash> syscoinBlockHashes = parseBlockHashes(reader);
        Keccak256Hash genesisParentHash = Keccak256Hash.wrap(new byte[32]); // initialised with 0s
        StoredBlock lastSyscoinBlock = syscoinWrapper.getBlock(syscoinBlockHashes.get(syscoinBlockHashes.size() - 1));

        return new SuperblockData(
                MerkleRootComputer.computeMerkleRoot(params, syscoinBlockHashes),
                syscoinBlockHashes,
                lastSyscoinBlock.getHeader().getTimeSeconds(),
                syscoinWrapper.getMedianTimestamp(lastSyscoinBlock),
                0,
                genesisParentHash,
                0);
    }

    private static List<Sha256Hash> parseBlockHashes(BufferedReader reader) throws IOException {
        List<Sha256Hash> result = new ArrayList<>();
        String line = reader.readLine();
        while (line != null) {
            byte[] rawBytes = HEX.decode(line);
            result.add(Sha256Hash.wrap(rawBytes));
            line = reader.readLine();
        }
        return result;
    }
}