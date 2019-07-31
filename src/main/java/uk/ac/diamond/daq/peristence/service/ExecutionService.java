package uk.ac.diamond.daq.peristence.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.peristence.data.Plan;
import uk.ac.diamond.daq.peristence.logging.ConfigurationLogService;
import uk.ac.diamond.daq.peristence.logging.LogToken;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ExecutionService {
    private static final Logger log = LoggerFactory.getLogger(ExecutionService.class);

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/mm/dd hh:MM:ss");
    private ConfigurationLogService configurationLogService;

    public ExecutionService(ConfigurationLogService configurationLogService) {
        this.configurationLogService = configurationLogService;
    }

    public void execute(Plan plan) throws PersistenceException {
        LogToken logToken = configurationLogService.logConfigurationChanges(plan, "Running plan "
                + plan.getName() + " at " + simpleDateFormat.format(new Date()));

        log.info("Created Log Token - {}", logToken.getDescription());

        plan.start();
    }
}
