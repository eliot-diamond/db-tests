package uk.ac.diamond.daq.persistence.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.data.ExecutableItem;
import uk.ac.diamond.daq.persistence.logging.ConfigurationLogService;
import uk.ac.diamond.daq.persistence.logging.LogToken;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ExecutionService {
    private static final Logger log = LoggerFactory.getLogger(ExecutionService.class);

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/mm/dd hh:MM:ss");
    private ConfigurationLogService configurationLogService;

    public ExecutionService(ConfigurationLogService configurationLogService) {
        this.configurationLogService = configurationLogService;
    }

    public void execute(ExecutableItem executableItem) throws PersistenceException {
        LogToken logToken = configurationLogService.logConfigurationChanges(executableItem, "Running plan "
                + executableItem.getName() + " at " + simpleDateFormat.format(new Date()));

        log.info("Created Log Token - {}", logToken.getDescription());

        executableItem.start();
    }
}
