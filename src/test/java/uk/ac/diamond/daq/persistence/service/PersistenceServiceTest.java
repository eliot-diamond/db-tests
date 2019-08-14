package uk.ac.diamond.daq.persistence.service;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.data.*;

import java.util.*;

import static org.junit.Assert.*;

public abstract class PersistenceServiceTest {
    private static final Logger log = LoggerFactory.getLogger(PersistenceServiceTest.class);

    private static final String COMMON_NAME = "Diff 1";
    private static final String CONCRETE_ITEM_B_NAME_1 = "Tomo Scan 1";
    private static final String CONCRETE_ITEM_B_NAME_2 = "Tomo Scan 2";
    private static final String CONCRETE_ITEM_CONTAINER_NAME = "Load Trigger";
    private static final String CONCRETE_ITEM_A_CLASS_UNIQUE = "Irn Bru";
    private static final long INVALID_ID = -1;

    protected PersistenceService persistenceService;

    private ConcreteItemA concreteItemA;
    private ConcreteItemB concreteItemB;
    private ConcreteItemA concreteItemA2;
    private ConcreteItemB concreteItemB2;

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

    @Test
    public void testDatabaseAddsId() throws PersistenceException {
        final ConcreteItemA unsavedItem = new ConcreteItemA("Diff 1", 0, 0, "Coffee");

        // When object is first created, its id should be set to an invalid value
        assertEquals("Invalid ID must be set", PersistableItem.INVALID_ID, unsavedItem.getId());

        // When persisted, the id should be updated to a valid value
        persistenceService.save(unsavedItem);
        assertNotEquals("Valid ID must be set", PersistableItem.INVALID_ID, unsavedItem.getId());
    }

    /**
     * Searching by an ID returns an object that matches the one that was persisted but a new memory reference
     * Searching by ID works for both the concrete class, any super class (Abstract or otherwise)
     * Retrieved items must also have different references than each other
     * Retrieved items should be deserialised into the subclass they went into the database as
     */
    @Test
    public void testSearchById() throws PersistenceException {
        ConcreteItemBsubA concreteItemB_A = new ConcreteItemBsubA("Name", 1, 1, 1);

        persistenceService.save(concreteItemB_A);

        final ConcreteItemBsubA retrievedSameClass = persistenceService.get(concreteItemB_A.getId(), ConcreteItemBsubA.class);
        final ConcreteItemB retrievedSuperClass = persistenceService.get(concreteItemB_A.getId(), ConcreteItemB.class);
        final AbstractItem retrievedAbstractSuperClass = persistenceService.get(concreteItemB_A.getId(), AbstractItem.class);

        assertNotSame("Different references required", concreteItemB_A, retrievedSameClass);
        assertEquals("Retrieved item must equals()", concreteItemB_A, retrievedSameClass);

        assertNotSame("Different references required", concreteItemB_A, retrievedSuperClass);
        assertEquals("Retrieved item must equals()", concreteItemB_A, retrievedSuperClass);

        assertNotSame("Different references required", concreteItemB_A, retrievedAbstractSuperClass);
        assertEquals("Retrieved item must equals()", concreteItemB_A, retrievedAbstractSuperClass);


        assertNotSame("Different references required", retrievedSameClass, retrievedSuperClass);
        assertNotSame("Different references required", retrievedSameClass, retrievedAbstractSuperClass);
        assertNotSame("Different references required", retrievedSuperClass, retrievedAbstractSuperClass);

        assertTrue("Not deserialised as B_A", retrievedSuperClass instanceof ConcreteItemBsubA);
        assertTrue("Not deserialised as B_A", retrievedAbstractSuperClass instanceof ConcreteItemBsubA);


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
        persistenceService.get(-1, PersistableItem.class);
    }

    @Test(expected = PersistenceException.class)
    public void testSearchIdForContents() throws PersistenceException {
        ConcreteItemContainer thisContainer = new ConcreteItemContainer("Name", concreteItemB, 12);
        persistenceService.save(thisContainer);

        persistenceService.get(concreteItemB.getId(), ConcreteItemContainer.class);
    }

    @Test(expected = PersistenceException.class)
    public void testSearchIdForContainer() throws PersistenceException {
        ConcreteItemContainer thisContainer = new ConcreteItemContainer("Name", concreteItemB, 12);
        persistenceService.save(thisContainer);

        persistenceService.get(thisContainer.getId(), ConcreteItemB.class);
    }

    /**
     * You persist a Tomography scan, then search for *all scans* and retrieve your scan cast to Scan.
     * You make changes to the scan and persist it once more.
     * ID should be maintained as deserialiser ensures that it comes out and goes in as a Tomography scan
     */
    @Test
    public void testCastingToSuperAndID() throws PersistenceException {
        ConcreteItemBsubA subClass = new ConcreteItemBsubA("sub", 2, 4, 8);
        persistenceService.save(subClass);

        ConcreteItemB superClass = persistenceService.get(subClass.getId(), ConcreteItemB.class);

        assertNotSame("Different references required", subClass, superClass);
        assertEquals("Either got wrong item or deserialised to wrong subclass", subClass, superClass);


        superClass.setProperty1(4);
        persistenceService.save(superClass);

        assertEquals("Same ID required", subClass.getId(), superClass.getId());
        assertTrue("Item should have been deserialised as subclass.", superClass instanceof ConcreteItemBsubA);

        final Map<String, String> searchParameters = new HashMap<>();
        searchParameters.put(AbstractItem.SEARCH_NAME_FIELD, "sub");

        final SearchResult searchResult = persistenceService.get(searchParameters, ConcreteItemBsubA.class);
        assertEquals("Find one item as casting does not change Id", 1, searchResult.getRows().size());

    }

    @Test
    public void testCastingToSuperAndChangingID() throws PersistenceException {
        ConcreteItemBsubA subClass = new ConcreteItemBsubA("sub", 2, 4, 8);
        persistenceService.save(subClass);

        ConcreteItemB superClass = persistenceService.get(subClass.getId(), ConcreteItemB.class);

        assertNotSame("Different references required", subClass, superClass);
        assertEquals("Either got wrong item or deserialised to wrong subclass", subClass, superClass);


        superClass.setName("newName");
        persistenceService.save(superClass);

        assertNotEquals("Different ID required", subClass.getId(), superClass.getId());
        assertTrue("Item should have been deserialised as subclass.", superClass instanceof ConcreteItemBsubA);

        final Map<String, String> searchParameters = new HashMap<>();
        searchParameters.put(AbstractItem.SEARCH_NAME_FIELD, "sub");

        final SearchResult searchResult = persistenceService.get(ConcreteItemBsubA.class);
        assertEquals("Changing name should change id but not type", 2, searchResult.getRows().size());

    }

    /**
     * You persist a Tomography scan, then search for *all scans* and retrieve your scan cast to Scan.
     * You make changes to the scan and persist it once more.
     * Should still be searchable for on the Tomography specific fields, as deserialiser ensures everything is stored
     * as itself, not as superclass.
     */
    @Test
    public void testSubclassSpecificFields() throws PersistenceException {

        final int SEARCH_VALUE = 8;

        ConcreteItemBsubA subClass = new ConcreteItemBsubA("sub", 2, 4, SEARCH_VALUE);
        persistenceService.save(subClass);

        ConcreteItemB superClass = persistenceService.get(subClass.getId(), ConcreteItemB.class);

        superClass.setProperty1(4);
        superClass.setName("newName"); // Otherwise would overwrite old one
        persistenceService.save(superClass);

        final Map<String, String> searchParameters = new HashMap<>();
        searchParameters.put(ConcreteItemBsubA.UNIQUE_FIELD, String.valueOf(SEARCH_VALUE));
        final SearchResult searchResult2 = persistenceService.get(searchParameters, ConcreteItemBsubA.class);

        assertEquals("Find both items as both stored as subclass.", 2, searchResult2.getRows().size());

        final SearchResult searchResult3 = persistenceService.get(searchParameters, ConcreteItemB.class);
        assertEquals("Find both items as both stored as subclass.", 2, searchResult3.getRows().size());
    }

    @Test
    public void testSearchForAllScans() throws PersistenceException {
        // Search for all scans
        final SearchResult searchResult = persistenceService.get(AbstractItem.class);
        assertEquals("Not all persisted items returned, expected A, A2, B, B2", 4, searchResult.getRows().size());

        printSearchResults("All Items", searchResult);

        final Set<SearchResultHeading> headings = searchResult.getHeadings();
        //(Id) + Name, 1 (A+B), 2, 3 (B), 3 (A)
        // As ConcreteA has a @Listable on property3 it shouldn't join wtih ConcreteB property3 but the property 1s should
        printSearchResults("All Items", searchResult);
        assertEquals("Expected 5 results: Name, Property 1, 2, 3B, 3A (as uniqueProperty); same field name should join unless listable under a different name", 5, headings.size());

        final SearchResult searchResult2 = persistenceService.get(ConcreteItemA.class);
        assertEquals("Expected 2 results", 2, searchResult2.getRows().size());
        final Set<SearchResultHeading> headings2 = searchResult2.getHeadings();
        //(Id) + ConcreteA 1, Concrete A 2, Concrete A3, Name
        assertEquals("Not all headings created", 4, headings2.size());
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

        assertEquals("Expected 3 results: B, B2, subClass", 3, searchResult2.getRows().size());
        //(Id) + ConcreteB 1, Concrete B3, Concrete B_A 4, Name
        assertEquals("Not all headings created", 4, headings2.size());
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
        //(Id) + ConcreteB 1, Concrete B3, Name
        assertEquals("Not all headings created", 3, searchResult.getHeadings().size());
        final SearchResult searchResult2 = persistenceService.get(searchParameters, ConcreteItemB.class);
        assertEquals("Only one item should be found", 1, searchResult2.getRows().size());
        //(Id) + ConcreteB 1, Concrete B3, Name
        assertEquals("Not all headings created", 3, searchResult2.getHeadings().size());
        final SearchResult searchResult3 = persistenceService.get(searchParameters, ConcreteItemA.class);
        assertEquals("No items should be found as looking for wrong class", 0, searchResult3.getRows().size());
        assertEquals("Created the wrong headings", 0, searchResult3.getHeadings().size());

        final long idAll = searchResult.getRows().get(0).getPersistenceId();
        final long idClass = searchResult2.getRows().get(0).getPersistenceId();
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
        //(Id) + ConcreteA 1, ConcreteA 2, ConcreteA 3, Name
        assertEquals("Not all headings created", 4, searchResult.getHeadings().size());

        final SearchResult searchResult2 = persistenceService.get(searchParameters, AbstractItem.class);
        assertEquals("Only one item should be found", 1, searchResult2.getRows().size());
        //(Id) + ConcreteA 1, ConcreteA 2, ConcreteA 3, Name
        assertEquals("Not all headings created", 4, searchResult2.getHeadings().size());

        final long id = searchResult.getRows().get(0).getPersistenceId();
        final long id2 = searchResult2.getRows().get(0).getPersistenceId();
        assertEquals("Wrong item found when searching by concrete class", concreteItemA.getId(), id);
        assertEquals("Wrong item found when searching by abstract classs", concreteItemA.getId(), id2);
        printSearchResults("Search for " + ConcreteItemA.CLASS_UNIQUE_FIELD + ": " + CONCRETE_ITEM_A_CLASS_UNIQUE, searchResult);
    }

    /**
     * Can search by a field that is common to multiple classes and get differing numbers of results depending on how
     * filtering by class.
     */
    @Test
    public void testSearchGettingMultipleUsingCommon() throws PersistenceException {
        final Map<String, String> searchParameters = new HashMap<>();
        searchParameters.put(AbstractItem.SEARCH_NAME_FIELD, COMMON_NAME);

        final SearchResult searchResult = persistenceService.get(searchParameters, AbstractItem.class);
        assertEquals("Should return all 3 items with the name", 3, searchResult.getRows().size());
        //(Id) + ConcreteA 1, ConcreteA 2, ConcreteA 3, ConcreteB 1, Concrete B 3, Name
        assertEquals("Not all headings created", 5, searchResult.getHeadings().size());

        List<Long> resultIds = new ArrayList<>();
        for (SearchResultRow result : searchResult.getRows()) {
            resultIds.add(result.getPersistenceId());
        }

        assertTrue(resultIds.contains(concreteItemA.getId()));
        assertTrue(resultIds.contains(concreteItemA2.getId()));
        assertTrue(resultIds.contains(concreteItemB2.getId()));

        final SearchResult searchResult2 = persistenceService.get(searchParameters, ConcreteItemA.class);
        assertEquals("Should return both ConcreteItemAs with the name", 2, searchResult2.getRows().size());
        //(Id) + ConcreteA 1, ConcreteA 2, ConcreteA 3, Name
        assertEquals("Not all headings created", 4, searchResult2.getHeadings().size());

        resultIds = new ArrayList<>();
        for (SearchResultRow result : searchResult2.getRows()) {
            resultIds.add(result.getPersistenceId());
        }
        assertTrue(resultIds.contains(concreteItemA.getId()));
        assertTrue(resultIds.contains(concreteItemA2.getId()));

        printSearchResults("Search for " + AbstractItem.SEARCH_NAME_FIELD + ": " + COMMON_NAME, searchResult);
    }

    /**
     * Can search by a field that is unique to class and get multiple results
     */
    @Test
    public void testSearchGettingMultipleUsingUnique() throws PersistenceException {
        final Map<String, String> searchParameters = new HashMap<>();
        concreteItemA2 = new ConcreteItemA(COMMON_NAME, 12, 23, CONCRETE_ITEM_A_CLASS_UNIQUE);
        persistenceService.save(concreteItemA2);
        searchParameters.put(ConcreteItemA.CLASS_UNIQUE_FIELD, CONCRETE_ITEM_A_CLASS_UNIQUE);

        final SearchResult searchResult = persistenceService.get(searchParameters, AbstractItem.class);
        assertEquals("Should return 2 items with the 'unique' value", 2, searchResult.getRows().size());
        //(Id) + ConcreteA 1, ConcreteA 2, ConcreteA 3, Name
        assertEquals("Not all headings created", 4, searchResult.getHeadings().size());

        List<Long> resultIds = new ArrayList<>();
        for (SearchResultRow result : searchResult.getRows()) {
            resultIds.add(result.getPersistenceId());
        }

        assertTrue(resultIds.contains(concreteItemA.getId()));
        assertTrue(resultIds.contains(concreteItemA2.getId()));

        final SearchResult searchResult2 = persistenceService.get(searchParameters, ConcreteItemA.class);
        assertEquals("Should return both ConcreteItemAs with the 'unique''", 2, searchResult2.getRows().size());
        //(Id) + ConcreteA 1, ConcreteA 2, ConcreteA 3, Name
        assertEquals("Not all headings created", 4, searchResult.getHeadings().size());

        resultIds = new ArrayList<>();
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

    /**
     * Expected behaviour for next 3 tests: changing the version of a contained object does not change the version of the container
     * However, changing an object (id, name, etc.) does change the version of the container.
     */
    /*
    @Test
    public void testVersionIncrementedOnEditContentsOfList() throws PersistenceException {
        final ConcreteItemContainer trigger1 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME, concreteItemB, 78);
        final ConcreteItemContainer trigger2 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME, concreteItemA, 78);


        persistenceService.save(trigger1);
        persistenceService.save(trigger2);
        final long originalVersion = trigger2.getVersion();
        final long consistentVersion = trigger1.getVersion();
        concreteItemA.setName("newName");
        concreteItemB.setProperty1(2222);
        persistenceService.save(trigger1);
        persistenceService.save(trigger2);

        assertTrue("Contained item did not increment version", concreteItemB.getId() > 0);
        assertTrue("Version of List did not increment on change of versionable value of its contents", trigger1.getVersion() > originalVersion);
        assertTrue("Version of List incremented on change of non-versionable value of its contents", trigger2.getVersion() == consistentVersion);

        trigger2.setAbstractItem(concreteItemA2);
        persistenceService.save(trigger2);

        assertTrue("Version of List did not increment when its contents changed", trigger2.getVersion() > consistentVersion);


    }

    @Test
    public void testVersionIncrementedOnEditContentsOfNestedList() throws PersistenceException {
        final ConcreteItemContainer trigger1 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME, concreteItemB, 78);
        final ConcreteItemContainer trigger2 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME, concreteItemA, 178);
        final ConcreteListContainer plan1 = new ConcreteListContainer("Name");
        persistenceService.save(plan1);

        long originalPlanVersion = plan1.getVersion();
        long originalTriggerVersion = trigger1.getVersion();

        long consistentItemVersion = concreteItemA.getVersion();
        long consistentTriggerVersion = trigger2.getVersion();

        concreteItemB.setName("newName");
        persistenceService.save(plan1);

        assertEquals("List contained within updated list changed version without change", consistentTriggerVersion, trigger2.getVersion());
        assertEquals("Item contained within updated list changed version without change", consistentItemVersion, concreteItemA.getVersion());

        assertTrue("Top level list did not increment version on contents change", plan1.getVersion() > originalPlanVersion);
        assertTrue("Contained list did not increment version on contents change", trigger1.getVersion() > originalTriggerVersion);

        originalPlanVersion = plan1.getVersion();
        originalTriggerVersion = trigger1.getVersion();
        consistentItemVersion = concreteItemB.getVersion();

        trigger2.setAbstractItem(concreteItemA2);
        persistenceService.save(plan1);

        assertTrue("Top level list did not increment version on contents change", plan1.getVersion() > originalPlanVersion);
        assertTrue("Contained list did not increment version on contents change", trigger2.getVersion() > consistentItemVersion);

        assertEquals("List contained within updated list changed version without change", originalTriggerVersion, trigger1.getVersion());
        assertEquals("Item contained within updated list changed version without change", consistentItemVersion, concreteItemB.getVersion());


    }

    @Test
    public void testVersionIncrementedOnEditContentsOfMap() throws PersistenceException {
        assertEquals(0, concreteItemB.getVersion());

        ConcreteMapContainer concreteMapContainer1 = new ConcreteMapContainer("Map Container");
        concreteMapContainer1.addItem("itemB", concreteItemB);
        persistenceService.save(concreteMapContainer1);

        assertEquals(0, concreteItemB.getVersion());
        assertEquals(0, concreteMapContainer1.getVersion());

        concreteItemB.setProperty1(1);

        assertEquals(0, concreteItemB.getVersion());

        persistenceService.save(concreteMapContainer1);

        assertEquals(1, concreteItemB.getVersion());
        assertEquals(0, concreteMapContainer1.getVersion());

        concreteItemB.setName("newName");
        persistenceService.save(concreteMapContainer1);

        assertEquals(0, concreteItemB.getVersion());
        assertEquals(1, concreteMapContainer1.getVersion());

        concreteMapContainer1.addItem("item2", concreteItemA);
        persistenceService.save(concreteMapContainer1);
        assertEquals(2, concreteMapContainer1.getVersion());


    }
    */

    @Test
    public void testChangingNameCreatesNewId() throws PersistenceException {
        final long originalId = concreteItemB.getId();
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

        final long id = searchResult.getRows().get(0).getPersistenceId();
        assertEquals("Wrong item found", trigger1.getId(), id);
        final AbstractItemContainer abstractItemContainer2 = persistenceService.get(id, ConcreteItemContainer.class);
        abstractItemContainer2.execute();

        //Make sure equal but new reference
        assertEquals(abstractItemContainer2, trigger1);
        assertNotSame(abstractItemContainer2, trigger1);
    }

    @Test
    public void testContainerHoldingItemThatUpdates() throws PersistenceException {
        final ConcreteItemContainer trigger1 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME, concreteItemB, 78);

        persistenceService.save(trigger1);
        assertNotEquals("Item should be saved and therefore get an ID when the container is saved", AbstractItem.INVALID_ID, concreteItemB.getId());

        concreteItemB.setProperty1(7);
        persistenceService.save(concreteItemB);

        assertNotEquals("Item should be saved and therefore note a version increase when its container is saved", 0, concreteItemB.getVersion());

        final AbstractItemContainer retrievedOriginal = persistenceService.get(trigger1.getId(), AbstractItemContainer.class);
        assertEquals("Ought to retrieve latest version of item in container", concreteItemB.getVersion(), retrievedOriginal.getAbstractItem().getVersion());

    }

    /**
     * git
     * get(A) where A contains B, C and B and C both contain D should get a single reference for D.
     * D should be the latest version of D.
     */
    @Test
    public void twoContainersHoldingSameItemInAPlan() throws PersistenceException {
        final ConcreteItemContainer trigger1 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME, concreteItemB, 78);
        final ConcreteItemContainer trigger2 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME, concreteItemB, 39);

        persistenceService.save(trigger1);
        concreteItemB.setName("newItem");
        trigger2.setName("newContainerName");

        String planName = "Plan 1";
        ConcreteListContainer concreteListContainer1 = new ConcreteListContainer(planName);
        concreteListContainer1.addTrigger(trigger1);
        concreteListContainer1.addTrigger(trigger2);
        persistenceService.save(concreteListContainer1);

        ConcreteListContainer retrievedListContainer = persistenceService.get(concreteListContainer1.getId(), ConcreteListContainer.class);

        assertEquals("Saving a container should save all it contains and retrieving one should get the latest version", "newContainerName", retrievedListContainer.getAbstractItemContainers().get(1).getName());
        assertEquals("Saving a container should save all it contains and retrieving one should get the latest version", "newItem", retrievedListContainer.getAbstractItemContainers().get(0).getAbstractItem().getName());
        assertSame("Should be same items as both Triggers use ConcreteB should retrieve latest for both", retrievedListContainer.getAbstractItemContainers().get(0).getAbstractItem(), retrievedListContainer.getAbstractItemContainers().get(1).getAbstractItem());
    }

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
        assertNotNull("Failed to find Plan " + concreteListContainer2.getName());
        assertNotSame("Retrieved plan should have new reference", concreteListContainer1, concreteListContainer2);
        assertEquals("Trigger 1 wasn't persisted in the plan", abstractItemContainer1, concreteListContainer2.getAbstractItemContainers().get(0));
        assertEquals("Trigger 1 Scan wasn't persisted in the plan", concreteItemB, concreteListContainer2.getAbstractItemContainers().get(0).getAbstractItem());
    }

    /**
     * See also testSavingContainerSavesAllContainedItem and testTwoContainersHoldingItem:
     * Saving A which contains B should save B.
     * Retrieving A after B has been updated should return the updated B.
     */
    @Test
    public void saveMap() throws PersistenceException {
        ConcreteItemA unsavedItem = new ConcreteItemA("unsaved", 1, 2, "3");

        ConcreteMapContainer concreteMapContainer1 = new ConcreteMapContainer("Map Container");
        concreteMapContainer1.addItem("itemA", unsavedItem);
        concreteMapContainer1.addItem("itemB", concreteItemB);
        persistenceService.save(concreteMapContainer1);

        assertNotEquals("Saving a map did not save its contained items", AbstractItem.INVALID_ID, unsavedItem.getId());
        concreteItemB.setProperty1(12);
        persistenceService.save(concreteItemB);

        ConcreteMapContainer concreteMapContainer2 = persistenceService.get(concreteMapContainer1.getId(), ConcreteMapContainer.class);
        assertEquals("Retrieved an old version of an item contained in the map", 12, ((ConcreteItemB) concreteMapContainer2.getItem("itemB")).getProperty1());
    }

    @Test
    public void saveMapWithRepeatedItem() throws PersistenceException {
        ConcreteMapContainer theMap = new ConcreteMapContainer("a map");
        theMap.addItem("itemA", concreteItemA);
        theMap.addItem("theSameItem", concreteItemA);

        persistenceService.save(theMap);
        assertNotEquals("Map was not saved", AbstractItem.INVALID_ID, theMap.getId());

        ConcreteMapContainer retrievedMap = persistenceService.get(theMap.getId(), ConcreteMapContainer.class);
        assertEquals("Did not retrieve item that went in with map", concreteItemA, theMap.getItem("itemA"));
        assertSame("Should ensure only one memory reference per item to prevent confusion over update history", theMap.getItem("itemA"), theMap.getItem("theSameItem"));

    }

    /**
     * For next 9 tests:
     * Saving a blank null should except, as should retrieving an item that ought to contain an item that was
     * deleted from the database.
     * But persisting and retrieving items that contain nulls should work.
     */

    @Test(expected = PersistenceException.class)
    public void saveNulls() throws PersistenceException {

        persistenceService.save(null);
    }

    @Test
    public void saveContainedNull() throws PersistenceException {
        ConcreteItemContainer nullHolder = new ConcreteItemContainer("nullName", null, 0);
        persistenceService.save(nullHolder);

        persistenceService.get(nullHolder.getId(), ConcreteItemContainer.class);

    }

    @Test
    public void saveMapContainingNull() throws PersistenceException {
        ConcreteMapContainer concreteMapContainer1 = new ConcreteMapContainer("nullItem");
        concreteMapContainer1.addItem("itemA", null);
        persistenceService.save(concreteMapContainer1);

        persistenceService.get(concreteMapContainer1.getId(), ConcreteMapContainer.class);

    }

    @Test
    public void saveContainerContainingListContainingNull() throws PersistenceException {
        ConcreteListContainer concreteListContainer1 = new ConcreteListContainer("null Holder Holder");
        ConcreteItemContainer nullHolder = new ConcreteItemContainer("nullHolder", null, 0);

        concreteListContainer1.addTrigger(nullHolder);
        persistenceService.save(concreteListContainer1);

        ConcreteListContainer newList = persistenceService.get(concreteListContainer1.getId(), ConcreteListContainer.class);
        newList.getAbstractItemContainers();

    }

    @Test
    public void saveContainerContainingNullList() throws PersistenceException {
        ConcreteListContainer concreteListContainer1 = new ConcreteListContainer("null Holder");
        ConcreteItemContainer nullContainer = null;

        concreteListContainer1.addTrigger(nullContainer);
        persistenceService.save(concreteListContainer1);

        persistenceService.get(concreteListContainer1.getId(), ConcreteListContainer.class);

    }

    @Test(expected = PersistenceException.class)
    public void deleteDoesntRetrieve() throws PersistenceException{
        final long id = concreteItemA.getId();
        persistenceService.delete(concreteItemA.getId());
        persistenceService.get(id, ConcreteItemA.class);

    }

    @Test(expected = PersistenceException.class)
    public void deleteUsingID() throws PersistenceException{
        final long id = concreteItemA.getId();
        persistenceService.delete(id);
        persistenceService.get(id, ConcreteItemA.class);

    }

    @Test
    public void deleteUsingInvalidID() throws PersistenceException {
        assertFalse("ID with no object deleted something", persistenceService.delete(INVALID_ID));

    }

    @Test(expected = PersistenceException.class)
    public void deleteExceptsContainerRetrieval() throws PersistenceException {
        ConcreteItemContainer container = new ConcreteItemContainer("name", concreteItemA, 1);
        persistenceService.save(container);
        persistenceService.delete(concreteItemA.getId());
        persistenceService.get(container.getId(), ConcreteItemContainer.class);

    }

    @Test
    public void deleteExceptsContainerRetrievalThroughLayers() throws PersistenceException{
        //TODO: is this line needed?
        ConcreteItemContainer container = new ConcreteItemContainer("name", concreteItemA, 1);
        ConcreteListContainer containerContainer = new ConcreteListContainer("all-enveloping");
        persistenceService.save(containerContainer);
        assertTrue(persistenceService.delete(concreteItemA.getId()));
        persistenceService.get(containerContainer.getId(), ConcreteListContainer.class);
    }

    @Test(expected = PersistenceException.class)
    public void deleteExceptsMapRetrieval() throws PersistenceException {
        ConcreteMapContainer container = new ConcreteMapContainer("name");
        container.addItem("itemA", concreteItemA);
        persistenceService.save(container);
        persistenceService.delete(concreteItemA.getId());
        persistenceService.get(container.getId(), ConcreteMapContainer.class);

    }

    @Test
    public void getOldVersion() throws PersistenceException {
        concreteItemB.setProperty1(12);
        persistenceService.save(concreteItemB);

        ConcreteItemB oldVersion = persistenceService.getArchive(concreteItemB.getId(), 0, ConcreteItemB.class);

        assertNotEquals("Got new version from archive", 12, oldVersion.getProperty1());
    }

    @Test(expected = PersistenceException.class)
    public void getInvalidIdFromArchive() throws PersistenceException {

        persistenceService.getArchive(INVALID_ID, 0, AbstractItem.class);

    }

    @Test(expected = PersistenceException.class)
    public void getInvalidVersionFromArchive() throws PersistenceException {

        persistenceService.getArchive(concreteItemB.getId(), 1, AbstractItem.class);

    }

    @Test(expected = PersistenceException.class)
    public void getInvalidClassFromArchive() throws PersistenceException {

        persistenceService.getArchive(concreteItemB.getId(), 0, ConcreteItemA.class);

    }

    @Test
    public void deleteDoesntEffectArchive() throws PersistenceException {
        concreteItemB.setProperty1(12);
        persistenceService.save(concreteItemB);

        assertTrue("Object not successfully deleted", persistenceService.delete(concreteItemB.getId()));

        List<Long> archivedVersions = persistenceService.getVersions(concreteItemB.getId());
        assertEquals("Should find all versions, even deleted ones, in archive", 2, archivedVersions.size());

        ConcreteItemB oldVersion = persistenceService.getArchive(concreteItemB.getId(), 0, ConcreteItemB.class);

        assertNotEquals("Got new version from archive", 12, oldVersion.getProperty1());

        assertNotNull("Couldn't find latest version after deletion", persistenceService.getArchive(concreteItemB.getId(), 1, ConcreteItemB.class));
    }

    @Test
    public void persistingIdenticalObjects() throws PersistenceException {

        ConcreteItemB concreteItemC = new ConcreteItemB(CONCRETE_ITEM_B_NAME_1, 100, 360.0); //functionality identical to concreteItemB
        ConcreteItemB concreteItemC_2 = new ConcreteItemB(CONCRETE_ITEM_B_NAME_1, 100, 360.0); //functionality identical to concreteItemB

        //As equality requires IDs to be the same, can only have 2 seperate items be equals prior to persisting
        assertEquals(concreteItemC_2, concreteItemC);


        ConcreteItemContainer containerA = new ConcreteItemContainer("hold_1", concreteItemC, 1);
        ConcreteItemContainer containerB = new ConcreteItemContainer("hold_2", concreteItemC_2, 1);

        ConcreteListContainer superContainer = new ConcreteListContainer("");

        superContainer.addTrigger(containerA);
        superContainer.addTrigger(containerB);

        persistenceService.save(superContainer);

        ConcreteListContainer retrievedContainer = persistenceService.get(superContainer.getId(), ConcreteListContainer.class);

        assertNotSame("Deserialised 2 identical but distinct items as the same item", retrievedContainer.getAbstractItemContainers().get(0).getAbstractItem(), retrievedContainer.getAbstractItemContainers().get(0).getAbstractItem());
        assertNotEquals("Two items written to same ID", concreteItemC_2, concreteItemC);

    }

    @Test
    public void persistingIdenticalObjectsInMap() throws PersistenceException {

        ConcreteItemB concreteItemC = new ConcreteItemB(CONCRETE_ITEM_B_NAME_1, 100, 360.0); //functionality identical to concreteItemB
        ConcreteItemB concreteItemC_2 = new ConcreteItemB(CONCRETE_ITEM_B_NAME_1, 100, 360.0); //functionality identical to concreteItemB

        //As equality requires IDs to be the same, can only have 2 seperate items be equals prior to persisting
        assertEquals(concreteItemC_2, concreteItemC);

        ConcreteMapContainer containerA = new ConcreteMapContainer("hold_1");
        containerA.addItem("Item1", concreteItemC);
        containerA.addItem("Item2", concreteItemC_2);


        persistenceService.save(containerA);

        ConcreteMapContainer retrievedContainer = persistenceService.get(containerA.getId(), ConcreteMapContainer.class);

        assertNotSame("Deserialised 2 identical but distinct items as the same item", retrievedContainer.getItem("Item1"), retrievedContainer.getItem("Item2"));
        assertNotEquals("Two items written to same ID", concreteItemC_2, concreteItemC);
        assertNotEquals("Two items written to same ID", retrievedContainer.getItem("Item1"), retrievedContainer.getItem("Item2"));


    }

    /**
     * For next 3 tests, ambiguity in which item would be saved with a higher version number
     * These cases are extremely fringe,as rely on id number and versioning both put in by the persistence
     * and other tests ensure that out of sync objects do not come out of the database.
     */
    /*
    //B2 is ~an unsaved change to B, B2 should be saved as a new version of B and be the one retrieved.
    @Test
    public void savingTwoOutOfSyncUnsavedItems() throws PersistenceException {

        concreteItemB2.setId(concreteItemB.getId());
        concreteItemB2.setVersion(concreteItemB.getVersion());


        ConcreteMapContainer containerA = new ConcreteMapContainer("hold_1");
        containerA.addItem("Item1", concreteItemB);
        containerA.addItem("Item2", concreteItemB2);

        persistenceService.save(containerA);

        ConcreteMapContainer retrived = persistenceService.get(containerA.getId(),ConcreteMapContainer.class);

        assertSame("Two references to same item", retrived.getItem("Item1"), retrived.getItem("Item2"));
        assertEquals("Did not persist latest unsaved changes", retrived.getItem("Item1"), concreteItemB2);

    }

    //B2 has a higher version but neither match the database, which should be saved later and therefore retrieved?
    //TODO: Probably neither, throw an except
    @Test(expected = PersistenceException.class)
    public void savingTwoOutOfSyncVersions() throws PersistenceException {

        concreteItemB2.setId(concreteItemB.getId());
        concreteItemB2.setVersion(concreteItemB.getVersion() + 1);

        concreteItemB.setProperty1(888);

        ConcreteMapContainer containerA = new ConcreteMapContainer("hold_1");
        containerA.addItem("Item1", concreteItemB);
        containerA.addItem("Item2", concreteItemB2);

        persistenceService.save(containerA);
    }

    @Test
    public void savingTwoOutOfSyncSavedItems() throws PersistenceException {

        concreteItemB2 = persistenceService.get(concreteItemB.getId(), ConcreteItemB.class);
        concreteItemB.setProperty1(7);
        persistenceService.save(concreteItemB); //B is now the higher version

        ConcreteMapContainer containerA = new ConcreteMapContainer("hold_1");
        containerA.addItem("Item1", concreteItemB);
        containerA.addItem("Item2", concreteItemB2);

        persistenceService.save(containerA);

        ConcreteMapContainer retrievedMap = persistenceService.get(containerA.getId(), ConcreteMapContainer.class);

        assertEquals("Should have saved all children and got most recent version", concreteItemB.getVersion(), retrievedMap.getItem("Item1"));
        assertEquals("Should have saved all children and got most recent version", concreteItemB.getVersion(), retrievedMap.getItem("Item2"));

    }
    */

    @Test
    public void arbitraryVersionChanges() throws PersistenceException {
        long originalVersion = concreteItemB.getVersion();
        concreteItemB2 = persistenceService.get(concreteItemB.getId(), ConcreteItemB.class);
        concreteItemB2.setProperty1(10000);
        concreteItemB.setVersion(500);
        concreteItemB.setProperty1(200);
        persistenceService.save(concreteItemB);
        persistenceService.save(concreteItemB2);

        assertTrue("Auto-set version number did not deal with arbitrary version changes/desync", concreteItemB.getVersion() > originalVersion);

        assertTrue("Auto-set version number did not deal with arbitrary version changes/desync", concreteItemB2.getVersion() > concreteItemB.getVersion());
    }

    @Test
    public void invalidVersionChanges() throws PersistenceException {
        concreteItemB2 = persistenceService.get(concreteItemB.getId(), ConcreteItemB.class);
        concreteItemB2.setProperty1(10000);
        concreteItemB2.setVersion(-5);
        persistenceService.save(concreteItemB2);
        assertEquals("Version set in item is ignored and the next version number is assigned by the service",
                concreteItemB.getVersion() + 1, concreteItemB2.getVersion());

    }

    //TODO: Or should all with invalid Ids be ignored except in archive?
//    @Test
//    public void invalidVersionOnlyInDatabase() throws PersistenceException {
//
//        concreteItemB2 = new ConcreteItemB("item", 1, 3);
//        concreteItemB2.setId(new BigInteger("1000")); //So no other item with ID
//        concreteItemB2.setVersion(-5);
//
//        persistenceService.save(concreteItemB2);
//        ConcreteItemB retrievedItem = persistenceService.get(concreteItemB2.getId(), ConcreteItemB.class);
//
//        assertTrue("Item with invalid version not set valid version on persist", retrievedItem.getVersion() > 0);
//        retrievedItem.setVersion(-5); //As equality of PersistedItem needs version, id to be the same.
//        assertEquals("Did not find item with invalid version when it is only instance of Id in database", concreteItemB2, retrievedItem);
//
//    }

}
