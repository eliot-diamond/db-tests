package uk.ac.diamond.daq.persistence.service;

import org.junit.Test;
import uk.ac.diamond.daq.persistence.data.ConcreteItemA;

import static org.junit.Assert.*;

public abstract class PersistenceManagementServiceTest {
    private static final String FIRST_VISIT = "First Visit";
    private static final String SECOND_VISIT = "Second Visit";
    private static final String THIRD_VISIT = "Third Visit";

    PersistenceService persistenceService;
    PersistenceManagementService persistenceManagementService;
    VisitService visitService;

    @Test
    public void changeVisitAndSaveSameItem() throws PersistenceException {
        visitService.setCurrentVisitId(FIRST_VISIT);

        ConcreteItemA itemA1 = new ConcreteItemA("Item A", 23, 65, "Only one");
        persistenceService.save(itemA1);
        long firstVisitId = itemA1.getId();

        visitService.setCurrentVisitId(SECOND_VISIT);

        persistenceService.save(itemA1);
        long secondVisitId = itemA1.getId();

        visitService.setCurrentVisitId(FIRST_VISIT);
        ConcreteItemA itemA2 = persistenceService.get(firstVisitId, ConcreteItemA.class);
        visitService.setCurrentVisitId(SECOND_VISIT);
        ConcreteItemA itemA3 = persistenceService.get(secondVisitId, ConcreteItemA.class);

        assertNotEquals("IDs should not be the same", itemA2.getId(), itemA3.getId());
        assertNotSame("Pointers should not the same", itemA2, itemA3);
        assertEquals("Names should be the same", itemA2.getName(), itemA3.getName());
    }

    public void copySimpleItemBetweenVisits() throws PersistenceException {
        visitService.setCurrentVisitId(FIRST_VISIT);

        ConcreteItemA itemA1 = new ConcreteItemA("Item A", 23, 65, "Only one");
        persistenceService.save(itemA1);
        long firstVisitId = itemA1.getId();

        visitService.setCurrentVisitId(SECOND_VISIT);
        persistenceService.save(itemA1);

        persistenceManagementService.copy(firstVisitId, FIRST_VISIT, THIRD_VISIT);
        try {
            persistenceService.get(firstVisitId, ConcreteItemA.class);
            fail("Should not find id " + firstVisitId + " in " + SECOND_VISIT);
        } catch (PersistenceException e) {
            //do nothing as this has passed
        }

        visitService.setCurrentVisitId(FIRST_VISIT);
        ConcreteItemA itemA2 = persistenceService.get(firstVisitId, ConcreteItemA.class);
        visitService.setCurrentVisitId(THIRD_VISIT);
        ConcreteItemA itemA3 = persistenceService.get(firstVisitId, ConcreteItemA.class);

        assertEquals("ID's from both visits should be the same", itemA2.getId(), itemA3.getId());
    }
}
