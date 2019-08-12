package uk.ac.diamond.daq.persistence.service;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.data.*;

import java.math.BigInteger;
import java.util.*;

import static org.junit.Assert.*;

public abstract class PersistenceServiceTest {
    private static final Logger log = LoggerFactory.getLogger(PersistenceServiceTest.class);

    private static final String COMMON_NAME = "Diff 1";
    private static final String CONCRETE_ITEM_B_NAME_1 = "Tomo Scan 1";
    private static final String CONCRETE_ITEM_B_NAME_2 = "Tomo Scan 2";
    private static final String CONCRETE_ITEM_CONTAINER_NAME = "Load Trigger";
    private static final String CONCRETE_ITEM_A_CLASS_UNIQUE = "Irn Bru";
    private static final BigInteger INVALID_ID = new BigInteger("-1");

    protected PersistenceService persistenceService;

    private ConcreteItemA concreteItemA;
    private ConcreteItemB concreteItemB;
    private ConcreteItemA concreteItemA2;
    private ConcreteItemB concreteItemB2;

    protected void createTestData() throws PersistenceException {
        // Add a scan of each type
        concreteItemA = new ConcreteItemA(COMMON_NAME, 55, 12, CONCRETE_ITEM_A_CLASS_UNIQUE);
        persistenceService.save(concreteItemA);
        concreteItemB = new ConcreteItemB(CONCRETE_ITEM_B_NAME_1, 100, 360.0);
        persistenceService.save(concreteItemB);
        // Same name to get multiple results for a search
        concreteItemA2 = new ConcreteItemA(COMMON_NAME, 12, 23, "Tea");
        persistenceService.save(concreteItemA2);
        concreteItemB2 = new ConcreteItemB(COMMON_NAME, 63, 55);
        persistenceService.save(concreteItemB2);
    }

    private static void printSearchResults(String title, SearchResult searchResult) {
        Set<SearchResultHeading> headings = searchResult.getHeadings();
        StringBuilder message = new StringBuilder();
        message.append("\n").append(title).append(":\n\nId\t|Version");
        for (SearchResultHeading heading : headings) {
            message.append("\t|").append(heading.getTitle());
        }
        for (SearchResultRow row : searchResult.getRows()) {
            message.append("\n").append(row.getPersistenceId());
            message.append("\t|").append(row.getVersion());
            for (SearchResultHeading heading : headings) {
                String value = row.getValues().get(heading);
                if (value == null) {
                    value = "";
                }
                message.append("\t|").append(value);
            }
        }
        log.info(message.toString());
    }

    @Test
    public void testDatabaseAddsId() throws PersistenceException {
        final ConcreteItemA diffractionScan = new ConcreteItemA("Diff 1", 0, 0, "Coffee");

        // When object is first created, its id should be set to an invalid value
        assertEquals("Invalid ID must be set", PersistableItem.INVALID_ID, diffractionScan.getId());

        // When persisted, the id should be updated to a valid value
        persistenceService.save(diffractionScan);
        assertNotEquals("Valid ID must be set", PersistableItem.INVALID_ID, diffractionScan.getId());
    }

    /**
     * Searching by an ID returns an object that matches the one that was persisted but a new memory reference
     * Searching by ID works for both the concrete class, any super class (Abstract or otherwise)
     * Retrieved items must also have different references than each other
     */
    @Test
    public void testSearchById() throws PersistenceException {
        ConcreteItemBsubA concreteItemB_A = new ConcreteItemBsubA("Name", 1, 1, 1);

        persistenceService.save(concreteItemB_A);

        final ConcreteItemBsubA retrievedSameClass = persistenceService.get(concreteItemB_A.getId(), ConcreteItemBsubA.class);
        final ConcreteItemBsubA retrievedSuperClass = (ConcreteItemBsubA) persistenceService.get(concreteItemB_A.getId(), ConcreteItemB.class);
        final ConcreteItemBsubA retrievedAbstractSuperClass = (ConcreteItemBsubA) persistenceService.get(concreteItemB_A.getId(), AbstractItem.class);

        assertNotSame("Different references required", concreteItemB_A, retrievedSameClass);
        assertEquals("Retrieved item must equals()", concreteItemB_A, retrievedSameClass);

        assertNotSame("Different references required", concreteItemB_A, retrievedSuperClass);
        assertEquals("Retrieved item must equals()", concreteItemB_A, retrievedSuperClass);

        assertNotSame("Different references required", concreteItemB_A, retrievedAbstractSuperClass);
        assertEquals("Retrieved item must equals()", concreteItemB_A, retrievedAbstractSuperClass);


        assertNotSame("Different references required", retrievedSameClass, retrievedSuperClass);
        assertNotSame("Different references required", retrievedSameClass, retrievedAbstractSuperClass);
        assertNotSame("Different references required", retrievedSuperClass, retrievedAbstractSuperClass);

    }

    @Test(expected = PersistenceException.class)
    public void testWrongClassResults() throws PersistenceException {
        //Searches for wrong class, so no objects found (but object with that ID does exist in database)
        persistenceService.get(concreteItemB.getId(), ConcreteItemA.class);
    }

    @Test(expected = PersistenceException.class)
    public void testSubClassResults() throws PersistenceException {
        //Searches for subclass, so no objects found (but object with that ID does exist in database)
        persistenceService.get(concreteItemB.getId(), ConcreteItemBsubA.class);
    }

    @Test(expected = PersistenceException.class)
    public void testNothingOfID() throws PersistenceException {
        //Searches for an ID which will not exist in the DB, uses most basic class i.e. PersistableItem to ensure checking all
        persistenceService.get(new BigInteger("-1"), PersistableItem.class);
    }

    @Test
    /**
     * You persist a Tomography scan, then search for *all scans* and retrieve your scan cast to Scan.
     * You make changes to the scan and persist it once more.
     * Should ID be maintained [and hence version incremented] or should it have a new ID [and hence version=0]?
     * Should it still be retrievable as a Tomo scan?
     */
    public void testCastingToSuperAndID() throws PersistenceException {
        ConcreteItemBsubA subClass = new ConcreteItemBsubA("sub", 2, 4, 8);
        persistenceService.save(subClass);

        ConcreteItemB superClass = persistenceService.get(subClass.getId(), ConcreteItemB.class);
        assertNotSame("Different references required", subClass, superClass);

        superClass.setProperty1(4);
        persistenceService.save(superClass);

        //TODO: See comment above
        assertEquals("Same ID required", subClass.getId(), superClass.getId());

        final Map<String, String> searchParameters = new HashMap<>();
        searchParameters.put(AbstractItem.SEARCH_NAME_FIELD, "sub");

        final SearchResult searchResult = persistenceService.get(searchParameters, ConcreteItemBsubA.class);
        assertEquals("Should only find the item stored as the subclass", 1, searchResult.getRows().size());

    }


    /**
     * You persist a Tomography scan, then search for *all scans* and retrieve your scan cast to Scan.
     * You make changes to the scan and persist it once more.
     * Should it still be searchable for on the Tomography specific fields?
     */
    @Test
    public void testSubclassSpecificFields() throws PersistenceException {

        final int SEARCH_VALUE = 8;

        ConcreteItemBsubA subClass = new ConcreteItemBsubA("sub", 2, 4, SEARCH_VALUE);
        persistenceService.save(subClass);

        ConcreteItemB superClass = persistenceService.get(subClass.getId(), ConcreteItemB.class);

        superClass.setProperty1(4);
        persistenceService.save(superClass);

        final Map<String, String> searchParameters = new HashMap<>();
        searchParameters.put(ConcreteItemBsubA.UNIQUE_FIELD, String.valueOf(SEARCH_VALUE));
        final SearchResult searchResult2 = persistenceService.get(searchParameters, ConcreteItemBsubA.class);
        assertEquals("Should only find the item stored as the subclass", 1, searchResult2.getRows().size());

        //TODO Should this get subclass only (because has field), sub + cast to super (because has field if cast) or
        // neither because super doesn't have field?
        final SearchResult searchResult3 = persistenceService.get(searchParameters, ConcreteItemB.class);
        assertEquals("Should only find the item stored as the subclass", 1, searchResult3.getRows().size());
    }


    @Test
    public void testSearchForAllScans() throws PersistenceException {
        // Search for all scans
        final SearchResult searchResult = persistenceService.get(AbstractItem.class);
        assertEquals("Not all persisted items returned, expected A, A2, B, B2", 4, searchResult.getRows().size());

        printSearchResults("All Items", searchResult);

        final Set<SearchResultHeading> headings = searchResult.getHeadings();
        //(Name, Id) + ConcreteA 1, Concrete A 2, Concrete A3, Concrete B 1, Concrete B 3
        assertEquals("Expected 5 results: TypeA 1,2,3; Type B 1,3: Type B properties 1,3 are different than type A properties, even though they share a name and (in the case of prop 1) same type.", 5, headings.size());

        printSearchResults("All Items", searchResult);

        final SearchResult searchResult2 = persistenceService.get(ConcreteItemA.class);
        assertEquals("Expected 2 results", 2, searchResult2.getRows().size());
        final Set<SearchResultHeading> headings2 = searchResult.getHeadings();
        //(Name, Id) + ConcreteA 1, Concrete A 2, Concrete A3
        assertEquals("Not all headings created", 3, headings2.size());
        printSearchResults("All ConcreteA Items", searchResult);

    }

    /**
     * Searching for ConcreteItemB also returns all ConcreteItemBsubAs- listable properties of subAs should be shown
     * Matches behaviour of getting all listable fields for e.g. searching by Abstract class.
     */
    @Test
    public void testSearchIncSubClasses() throws PersistenceException {

        ConcreteItemBsubA subClass = new ConcreteItemBsubA("sub", 2, 4, 8);
        persistenceService.save(subClass);
        final SearchResult searchResult2 = persistenceService.get(ConcreteItemB.class);
        final Set<SearchResultHeading> headings2 = searchResult2.getHeadings();

        printSearchResults("B items including B_A", searchResult2);

        assertEquals("Expected 3 results: A, A2, subClass", 3, searchResult2.getRows().size());
        //(Name, Id) + ConcreteB 1, Concrete B3, Concrete B_A 4
        assertEquals("Not all headings created", 3, headings2.size());

    }

    /**
     * Can search by a field that is common to multiple classes and get the unique result
     */
    @Test
    public void testSearchUsingCommonParameter() throws PersistenceException {
        final Map<String, String> searchParameters = new HashMap<>();
        searchParameters.put(AbstractItem.SEARCH_NAME_FIELD, CONCRETE_ITEM_B_NAME_1);

        final SearchResult searchResult = persistenceService.get(searchParameters, AbstractItem.class);
        assertEquals("Only one item should be found", 1, searchResult.getRows().size());
        final SearchResult searchResult2 = persistenceService.get(searchParameters, ConcreteItemB.class);
        assertEquals("Only one item should be found", 1, searchResult2.getRows().size());
        final SearchResult searchResult3 = persistenceService.get(searchParameters, ConcreteItemA.class);
        assertEquals("No items should be found as looking for wrong class", 0, searchResult3.getRows().size());

        final BigInteger idAll = searchResult.getRows().get(0).getPersistenceId();
        final BigInteger idClass = searchResult.getRows().get(0).getPersistenceId();
        assertEquals("Wrong item found when searching for all", concreteItemB.getId(), idAll);
        assertEquals("Wrong item found when searching only class", concreteItemB.getId(), idClass);

        printSearchResults("Search for " + CONCRETE_ITEM_B_NAME_1, searchResult);
    }

    /**
     * Can search by a field unique to one class and get a single result
     */
    @Test
    public void testSearchUsingUniqueParameter() throws PersistenceException {
        final Map<String, String> searchParameters = new HashMap<>();
        searchParameters.put(ConcreteItemA.CLASS_UNIQUE_FIELD, CONCRETE_ITEM_A_CLASS_UNIQUE);

        //For just the classes that have the unique field and also for classes that do not
        final SearchResult searchResult = persistenceService.get(searchParameters, ConcreteItemA.class);
        assertEquals("Only one item should be found", 1, searchResult.getRows().size());

        final SearchResult searchResult2 = persistenceService.get(searchParameters, AbstractItem.class);
        assertEquals("Only one item should be found", 1, searchResult2.getRows().size());

        final BigInteger id = searchResult.getRows().get(0).getPersistenceId();
        final BigInteger id2 = searchResult2.getRows().get(0).getPersistenceId();
        assertEquals("Wrong item found when searching by concrete class", concreteItemA.getId(), id);
        assertEquals("Wrong item found when searching by abstract classs", concreteItemA.getId(), id2);
        printSearchResults("Search for " + ConcreteItemA.CLASS_UNIQUE_FIELD + ": " + CONCRETE_ITEM_A_CLASS_UNIQUE, searchResult);
    }

    /**
     * Can search by a field that is common to multiple classes and get differing numbers of results depending on how
     * filtering by class.
     */
    @Test
    public void testSearchGettingMultiple() throws PersistenceException {
        final Map<String, String> searchParameters = new HashMap<>();
        searchParameters.put(AbstractItem.SEARCH_NAME_FIELD, COMMON_NAME);

        final SearchResult searchResult = persistenceService.get(searchParameters, AbstractItem.class);
        assertEquals("Should return all 3 items with the name", 3, searchResult.getRows().size());

        List<BigInteger> resultIds = new ArrayList<BigInteger>();
        for (SearchResultRow result : searchResult.getRows()) {
            resultIds.add(result.getPersistenceId());
        }

        assertTrue(resultIds.contains(concreteItemA.getId()));
        assertTrue(resultIds.contains(concreteItemA2.getId()));
        assertTrue(resultIds.contains(concreteItemB2.getId()));

        final SearchResult searchResult2 = persistenceService.get(searchParameters, ConcreteItemA.class);
        assertEquals("Should return both ConcreteItemAs with the name", 2, searchResult2.getRows().size());

        resultIds = new ArrayList<BigInteger>();
        for (SearchResultRow result : searchResult2.getRows()) {
            resultIds.add(result.getPersistenceId());
        }
        assertTrue(resultIds.contains(concreteItemA.getId()));
        assertTrue(resultIds.contains(concreteItemA2.getId()));

        printSearchResults("Search for " + ConcreteItemA.CLASS_UNIQUE_FIELD + ": " + CONCRETE_ITEM_A_CLASS_UNIQUE, searchResult);
    }

    @Test
    public void testSaveRetrieveTrigger() throws PersistenceException {
        final AbstractItemContainer abstractItemContainer = new ConcreteItemContainer("Load Trigger", concreteItemB, 78);
        assertEquals("Invalid ID must be set", abstractItemContainer.getId(), PersistableItem.INVALID_ID);
        persistenceService.save(abstractItemContainer);
        assertNotEquals("Valid ID must be set", abstractItemContainer.getId(), PersistableItem.INVALID_ID);

        AbstractItemContainer retrievedContainer = persistenceService.get(abstractItemContainer.getId(), AbstractItemContainer.class);
        assertEquals("Retrieved item should equals()", abstractItemContainer, retrievedContainer);
        assertNotSame("Retrieved item should have new reference", abstractItemContainer, retrievedContainer);
    }

    @Test
    public void testVersionIncrementedOnEdit() throws PersistenceException {
        final long originalVersion = concreteItemB.getVersion();
        concreteItemB.setProperty1(101);
        persistenceService.save(concreteItemB);

        final List<Long> versions = persistenceService.getVersions(concreteItemB.getId());
        assertEquals("2 versions must be included", 2, versions.size());
        assertNotEquals("Version has not changed", concreteItemB.getVersion(), originalVersion);
        assertTrue("Version has not been incremented", concreteItemB.getVersion() > originalVersion);
    }

    @Test
    public void testChangingNameCreatesNewId() throws PersistenceException {
        final BigInteger originalId = concreteItemB.getId();
        concreteItemB.setProperty1(101);
        persistenceService.save(concreteItemB);

        //To make sure that when the version is reset it has obviously been non-zero
        assertTrue(concreteItemB.getVersion() > 0);
        concreteItemB.setName(CONCRETE_ITEM_B_NAME_2);
        persistenceService.save(concreteItemB);

        assertNotEquals("Persistence ID should have changed", concreteItemB.getId(), originalId);
        assertEquals("Version has been reset", 0, concreteItemB.getVersion());
    }

    @Test
    public void testGetReturnsNewObject() throws PersistenceException {
        final ConcreteItemB retrievedItem = persistenceService.get(concreteItemB.getId(), ConcreteItemB.class);
        assertNotSame("Different references required", concreteItemB, retrievedItem);
        assertEquals("Tomography Scan must equals()", concreteItemB, retrievedItem);
    }

    @Test
    public void testSearchForTrigger() throws PersistenceException {
        final ConcreteItemContainer trigger1 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME, concreteItemB, 78);
        persistenceService.save(trigger1);

        final Map<String, String> searchParameters = new HashMap<>();
        searchParameters.put(AbstractItemContainer.CONTAINER_NAME, CONCRETE_ITEM_CONTAINER_NAME);

        final SearchResult searchResult = persistenceService.get(searchParameters, AbstractItemContainer.class);
        printSearchResults("Search for " + CONCRETE_ITEM_CONTAINER_NAME, searchResult);
        assertEquals("Only one item should be found", 1, searchResult.getRows().size());

        final BigInteger id = searchResult.getRows().get(0).getPersistenceId();
        assertEquals("Wrong item found", trigger1.getId(), id);
        final AbstractItemContainer abstractItemContainer2 = persistenceService.get(id, ConcreteItemContainer.class);
        abstractItemContainer2.execute();

        //Make sure equal but new reference
        assertEquals(abstractItemContainer2, trigger1);
        assertNotSame(abstractItemContainer2, trigger1);
    }

    /**
     * Should we be able to find a container with the fields of what it contains?
     */
    @Test
    public void testSearchForContainerByItem() throws PersistenceException {
        final ConcreteItemContainer trigger1 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME, concreteItemB, 78);
        final ConcreteItemContainer trigger2 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME, concreteItemA, 78);

        persistenceService.save(trigger1);
        persistenceService.save(trigger2);

        final Map<String, String> searchParameters = new HashMap<>();
        searchParameters.put(AbstractItem.SEARCH_NAME_FIELD, CONCRETE_ITEM_B_NAME_1);

        final SearchResult searchResult = persistenceService.get(searchParameters, AbstractItemContainer.class);
        printSearchResults("Search for item within container with name " + concreteItemB.getName(), searchResult);
        //TODO: Do we want to be able to do this: search for a container by the name of its contents?
        assertEquals("Only one item should be found", 1, searchResult.getRows().size());

        final BigInteger id = searchResult.getRows().get(0).getPersistenceId();
        assertEquals("Wrong item found", trigger1.getId(), id);
        final AbstractItemContainer abstractItemContainer2 = persistenceService.get(id, ConcreteItemContainer.class);
        abstractItemContainer2.execute();

        //Make sure equal but new reference
        assertEquals(abstractItemContainer2, trigger1);
        assertNotSame(abstractItemContainer2, trigger1);
    }

    /**
     * Should saving a container save its items?
     * Should get(A) where A contains B get the latest B or the B that existed when A went into the database.
     * These tests may need to be rewritten, they were written with the assumption that saving a container saves the item
     * and that what goes in is what comes out.
     */
    @Test
    public void testContainerHoldingItemThatUpdates() throws PersistenceException {
        final ConcreteItemContainer trigger1 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME, concreteItemB, 78);

        persistenceService.save(trigger1);
        assertNotEquals("Item should be saved and therefore get an ID when the container is saved", AbstractItem.INVALID_ID, concreteItemB.getId());

        concreteItemB.setProperty1(7);
        persistenceService.save(concreteItemB);

        assertNotEquals("Item should be saved and therefore note a version increase when its container is saved", 0, concreteItemB.getVersion());

        final AbstractItemContainer retrievedOriginal = persistenceService.get(trigger1.getId(), AbstractItemContainer.class);
        //TODO: See comment on test method
        assertEquals("Ought to retrieve version of item that went into database with container", 0, retrievedOriginal.getAbstractItem().getVersion());

    }

    /**
     * Should saving a container save its items?
     * Should get(A) where A contains B get the latest B or the B that existed when A went into the database.
     * These tests may need to be rewritten, they were written with the assumption that saving a container saves the item
     * and that what goes in is what comes out.
     */
    @Test
    public void testTwoContainersHoldingItem() throws PersistenceException {
        final ConcreteItemContainer container1 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME, concreteItemB, 78);
        final ConcreteItemContainer container2 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME, concreteItemB, 87);

        persistenceService.save(container1);
        concreteItemB.setProperty1(100);
        persistenceService.save(container2);

        final AbstractItemContainer abstractItemContainer1 = persistenceService.get(container1.getId(), ConcreteItemContainer.class);
        final AbstractItemContainer abstractItemContainer2 = persistenceService.get(container2.getId(), ConcreteItemContainer.class);

        //TODO: See comment on test method
        assertEquals("The version that goes into the database within a container should be the version that comes out", 0, abstractItemContainer1.getAbstractItem().getVersion());

        assertNotEquals(abstractItemContainer1.getAbstractItem().getVersion(), abstractItemContainer2.getAbstractItem().getVersion());

        // Equality needs to be written in a way that two different versions of a held object are not equal
        assertNotEquals("Two containers with the same names, properties, items but different versions of a held item should not be equal", abstractItemContainer1, abstractItemContainer2);

        // Equality needs to be written in a way that two same versions of a held object are equal (when name, properties etc. are same)
        assertEquals("Two containers with the same names, properties, items and versions of items should be equal", abstractItemContainer2, container2);
        assertNotSame(abstractItemContainer2, container2);

        assertNotSame(abstractItemContainer2.getAbstractItem(), container2.getAbstractItem());
    }

    /**
     * Should saving a container save its items?
     * Should get(A) where A contains B, C and B and C both contain D ensure that D has the same version for both B, C?
     * These tests may need to be rewritten, they were written with the assumption above
     * Should B's D and C's D be the same item? Presumption is Yes.
     */
    @Test
    public void twoContainersHoldingSameItemInAPlan() throws PersistenceException {
        final ConcreteItemContainer trigger1 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME, concreteItemB, 78);
        final ConcreteItemContainer trigger2 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME, concreteItemB, 39);

        persistenceService.save(trigger1);
        concreteItemB.setProperty1(7);
        persistenceService.save(trigger2);

        String planName = "Plan 1";
        ConcreteListContainer concreteListContainer1 = new ConcreteListContainer(planName);
        concreteListContainer1.addTrigger(trigger1);
        concreteListContainer1.addTrigger(trigger2);
        persistenceService.save(concreteListContainer1);

        ConcreteListContainer retrievedListContainer = persistenceService.get(concreteListContainer1.getId(), ConcreteListContainer.class);
        assertEquals("Retrieved 2 different versions of the same object within a list", retrievedListContainer.getAbstractItemContainers().get(0).getAbstractItem().getVersion(), retrievedListContainer.getAbstractItemContainers().get(1).getAbstractItem().getVersion());
        assertSame("Have two different pointers to what should be the same item", retrievedListContainer.getAbstractItemContainers().get(0).getAbstractItem(), retrievedListContainer.getAbstractItemContainers().get(1).getAbstractItem());
    }

    /**
     * Should saving a container save its items?
     * Should get(A) where A contains B, C and B and C both contain D ensure that D has the same version for both B, C?
     * These tests may need to be rewritten, they were written with the assumption above
     * Should B's D and C's D be the same item? Presumption is Yes.
     */
    @Test
    public void twoContainersHoldingSimilarItemInAPlan() throws PersistenceException {
        final ConcreteItemContainer trigger1 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME, concreteItemB, 78);
        final ConcreteItemContainer trigger2 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME, concreteItemB, 39);

        persistenceService.save(trigger1);
        concreteItemB.setName("newItem");
        persistenceService.save(trigger2);

        String planName = "Plan 1";
        ConcreteListContainer concreteListContainer1 = new ConcreteListContainer(planName);
        concreteListContainer1.addTrigger(trigger1);
        concreteListContainer1.addTrigger(trigger2);
        persistenceService.save(concreteListContainer1);

        ConcreteListContainer retrievedListContainer = persistenceService.get(concreteListContainer1.getId(), ConcreteListContainer.class);
        assertNotSame("Should be different items as changing name changes ID", retrievedListContainer.getAbstractItemContainers().get(0).getAbstractItem(), retrievedListContainer.getAbstractItemContainers().get(1).getAbstractItem());
    }

    /**
     * Do we want persisting a container to persists its contained object?
     */
    @Test
    public void testSavingContainerSavesAllContainedItem() throws PersistenceException {
        concreteItemB.setName("toBeFound");
        concreteItemA.setName("toBeFound");
        final ConcreteItemContainer trigger1 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME, concreteItemB, 78);
        final ConcreteItemContainer trigger2 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME, concreteItemA, 78);
        persistenceService.save(trigger1);
        trigger1.setName("thisChanged");
        String planName = "Plan 1";
        ConcreteListContainer concreteListContainer1 = new ConcreteListContainer(planName);
        concreteListContainer1.addTrigger(trigger1);

        persistenceService.save(trigger2);
        persistenceService.save(concreteListContainer1);

        AbstractItemContainer retrivedContainer = persistenceService.get(trigger2.getId(), AbstractItemContainer.class);
        ConcreteListContainer retrivedListContainer = persistenceService.get(concreteListContainer1.getId(), ConcreteListContainer.class);

        assertEquals("Persisting a container did not persist the contained", "toBeFound", retrivedContainer.getAbstractItem().getName());
        assertEquals("Persisting a container did not persist the nested container", "thisChanged", retrivedListContainer.getAbstractItemContainers().get(0).getName());

        assertEquals("Persisting Container A which contained Container B which contained Item C did not persist Item C", "toBeFound", retrivedListContainer.getAbstractItemContainers().get(0).getAbstractItem().getName());
    }

    @Test
    public void createPlan() throws PersistenceException {
        AbstractItemContainer abstractItemContainer1 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME + " 1", concreteItemB, 78);
        persistenceService.save(abstractItemContainer1);
        AbstractItemContainer abstractItemContainer2 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME + " 2", concreteItemA, 99);
        persistenceService.save(abstractItemContainer2);

        String planName = "Plan 1";
        ConcreteListContainer concreteListContainer1 = new ConcreteListContainer(planName);
        concreteListContainer1.addTrigger(abstractItemContainer1);
        concreteListContainer1.addTrigger(abstractItemContainer2);
        persistenceService.save(concreteListContainer1);
        ConcreteListContainer concreteListContainer2 = persistenceService.get(concreteListContainer1.getId(), ConcreteListContainer.class);
        concreteListContainer2.execute();
        assertNotNull("Failed to find Plan " + concreteListContainer1.getName(), concreteListContainer2);
        assertEquals("Trigger 1", abstractItemContainer1, concreteListContainer2.getAbstractItemContainers().get(0));
        assertEquals("Trigger 1 Scan", concreteItemB, concreteListContainer2.getAbstractItemContainers().get(0).getAbstractItem());
    }

    /**
     * See also testSavingContainerSavesAllContainedItem and testTwoContainersHoldingItem:
     * Do we want saving A that holds B to save B?
     * Do we want retrieving A to retrieve the most recent B or the one that went into the persistence?
     */
    @Test
    public void saveMap() throws PersistenceException {
        ConcreteItemA unsavedItem = new ConcreteItemA("unsaved", 1, 2, "3");

        ConcreteMapContainer concreteMapContainer1 = new ConcreteMapContainer("Map Container");
        concreteMapContainer1.addItem("itemA", unsavedItem);
        concreteMapContainer1.addItem("itemB", concreteItemB);
        persistenceService.save(concreteMapContainer1);

        assertNotEquals("Saving a map did not save its contained items", AbstractItem.INVALID_ID, unsavedItem.getId());
        concreteItemB.setName("newName");
        persistenceService.save(concreteItemB);

        ConcreteMapContainer concreteMapContainer2 = persistenceService.get(concreteMapContainer1.getId(), ConcreteMapContainer.class);
        assertNotEquals("Retrieved a newer version of an item contained in the map than went in ", "newName", concreteMapContainer2.getItem("itemB").getName());
    }

}
