package org.molgenis.data.meta;

import com.google.common.collect.Lists;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.molgenis.data.DataService;
import org.molgenis.data.MolgenisDataException;
import org.molgenis.data.Repository;
import org.molgenis.data.RepositoryCollection;
import org.molgenis.data.meta.model.Attribute;
import org.molgenis.data.meta.model.EntityType;
import org.molgenis.data.meta.system.SystemEntityTypeRegistry;
import org.molgenis.test.AbstractMockitoTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.molgenis.data.meta.model.AttributeMetadata.ATTRIBUTE_META_DATA;
import static org.testng.Assert.assertEquals;

public class EntityTypeRepositoryDecoratorTest extends AbstractMockitoTest
{
	private final String entityTypeId1 = "EntityType1";
	private final String entityTypeId2 = "EntityType2";
	private final String entityTypeId3 = "EntityType3";
	private final String entityTypeId4 = "EntityType4";
	private EntityTypeRepositoryDecorator repo;
	@Mock
	private Repository<EntityType> decoratedRepo;
	@Mock
	private DataService dataService;
	@Mock
	private MetaDataService metaDataService;
	@Mock
	private SystemEntityTypeRegistry systemEntityTypeRegistry;
	@Mock
	private EntityType entityType1;
	@Mock
	private EntityType entityType2;
	@Mock
	private EntityType entityType3;
	@Mock
	private EntityType entityType4;

	@BeforeMethod
	public void setUpBeforeMethod()
	{
		when(dataService.getMeta()).thenReturn(metaDataService);
		when(entityType1.getId()).thenReturn(entityTypeId1);
		when(entityType1.getLabel()).thenReturn(entityTypeId1);
		when(entityType2.getId()).thenReturn(entityTypeId2);
		when(entityType2.getLabel()).thenReturn(entityTypeId1);
		when(entityType3.getId()).thenReturn(entityTypeId3);
		when(entityType3.getLabel()).thenReturn(entityTypeId1);
		when(entityType4.getId()).thenReturn(entityTypeId4);
		when(entityType4.getLabel()).thenReturn(entityTypeId1);
		repo = new EntityTypeRepositoryDecorator(decoratedRepo, dataService);
	}

	@Test
	public void addWithKnownBackend()
	{
		EntityType entityType = when(mock(EntityType.class).getId()).thenReturn("entity").getMock();
		when(entityType.getAttributes()).thenReturn(emptyList());
		String backendName = "knownBackend";
		when(entityType.getBackend()).thenReturn(backendName);
		MetaDataService metaDataService = mock(MetaDataService.class);
		RepositoryCollection repoCollection = mock(RepositoryCollection.class);
		when(metaDataService.getBackend(entityType)).thenReturn(repoCollection);
		when(dataService.getMeta()).thenReturn(metaDataService);
		repo.add(entityType);
		verify(decoratedRepo).add(entityType);
	}

	@Test(expectedExceptions = MolgenisDataException.class, expectedExceptionsMessageRegExp = "Unknown backend \\[unknownBackend\\]")
	public void addWithUnknownBackend()
	{
		EntityType entityType = when(mock(EntityType.class).getId()).thenReturn("entity").getMock();
		when(entityType.getAttributes()).thenReturn(emptyList());
		String backendName = "unknownBackend";
		when(entityType.getBackend()).thenReturn(backendName);
		MetaDataService metaDataService = mock(MetaDataService.class);
		when(metaDataService.getBackend(backendName)).thenReturn(null);
		when(dataService.getMeta()).thenReturn(metaDataService);
		repo.add(entityType);
		verify(decoratedRepo).add(entityType);
	}

	@Test
	public void delete()
	{
		EntityType entityType = when(mock(EntityType.class).getId()).thenReturn("entity").getMock();
		Attribute attr0 = mock(Attribute.class);
		when(attr0.getChildren()).thenReturn(emptyList());
		Attribute attrCompound = mock(Attribute.class);
		Attribute attr1a = mock(Attribute.class);
		when(attr1a.getChildren()).thenReturn(emptyList());
		Attribute attr1b = mock(Attribute.class);
		when(attr1b.getChildren()).thenReturn(emptyList());
		when(attrCompound.getChildren()).thenReturn(newArrayList(attr1a, attr1b));
		when(entityType.getOwnAttributes()).thenReturn(newArrayList(attr0, attrCompound));

		String backendName = "backend";
		when(entityType.getBackend()).thenReturn(backendName);
		RepositoryCollection repoCollection = mock(RepositoryCollection.class);
		when(metaDataService.getBackend(backendName)).thenReturn(repoCollection);

		repo.delete(entityType);

		verify(decoratedRepo).delete(entityType);
		verify(repoCollection).deleteRepository(entityType);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Stream<Attribute>> attrCaptor = ArgumentCaptor.forClass((Class) Stream.class);
		verify(dataService).delete(eq(ATTRIBUTE_META_DATA), attrCaptor.capture());
		assertEquals(attrCaptor.getValue().collect(toList()), newArrayList(attr0, attrCompound, attr1a, attr1b));
	}

	@Test
	public void deleteAbstract()
	{
		EntityType entityType = when(mock(EntityType.class).getId()).thenReturn("entity").getMock();
		when(entityType.isAbstract()).thenReturn(true);
		Attribute attr0 = mock(Attribute.class);
		when(attr0.getChildren()).thenReturn(emptyList());
		when(entityType.getOwnAttributes()).thenReturn(singletonList(attr0));

		String backendName = "backend";
		when(entityType.getBackend()).thenReturn(backendName);
		RepositoryCollection repoCollection = mock(RepositoryCollection.class);
		when(metaDataService.getBackend(backendName)).thenReturn(repoCollection);

		repo.delete(entityType);

		verify(decoratedRepo).delete(entityType);
		verify(repoCollection, times(0)).deleteRepository(entityType); // entity is abstract

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Stream<Attribute>> attrCaptor = ArgumentCaptor.forClass((Class) Stream.class);
		verify(dataService).delete(eq(ATTRIBUTE_META_DATA), attrCaptor.capture());
		assertEquals(attrCaptor.getValue().collect(toList()), singletonList(attr0));
	}

	@Test
	public void update()
	{
		when(entityType1.getIdValue()).thenReturn(entityTypeId1);
		when(entityType2.getIdValue()).thenReturn(entityTypeId2);
		when(entityType3.getIdValue()).thenReturn(entityTypeId3);
		when(entityType4.getIdValue()).thenReturn(entityTypeId4);

		EntityType currentEntityType = mock(EntityType.class);
		EntityType currentEntityType2 = mock(EntityType.class);
		EntityType currentEntityType3 = mock(EntityType.class);
		when(systemEntityTypeRegistry.getSystemEntityType(entityTypeId1)).thenReturn(null);
		when(decoratedRepo.findOneById(entityTypeId1)).thenReturn(currentEntityType);
		when(decoratedRepo.findOneById(entityTypeId2)).thenReturn(currentEntityType2);
		when(decoratedRepo.findOneById(entityTypeId3)).thenReturn(currentEntityType3);

		Attribute attributeStays = mock(Attribute.class);
		when(attributeStays.getName()).thenReturn("attributeStays");
		Attribute attributeRemoved = mock(Attribute.class);
		when(attributeRemoved.getName()).thenReturn("attributeRemoved");
		Attribute attributeAdded = mock(Attribute.class);
		when(attributeAdded.getName()).thenReturn("attributeAdded");

		when(currentEntityType.getOwnAllAttributes()).thenReturn(Lists.newArrayList(attributeStays, attributeRemoved));
		when(entityType1.getOwnAllAttributes()).thenReturn(Lists.newArrayList(attributeStays, attributeAdded));
		when(metaDataService.getConcreteChildren(entityType1)).thenReturn(Stream.of(entityType2, entityType3));
		RepositoryCollection backend2 = mock(RepositoryCollection.class);
		RepositoryCollection backend3 = mock(RepositoryCollection.class);
		when(metaDataService.getBackend(entityType2)).thenReturn(backend2);
		when(metaDataService.getBackend(entityType3)).thenReturn(backend3);

		repo.update(entityType1);

		// verify that attributes got added and deleted in concrete extending entities
		verify(backend2).addAttribute(currentEntityType2, attributeAdded);
		verify(backend2).deleteAttribute(currentEntityType2, attributeRemoved);
		verify(backend3).addAttribute(currentEntityType3, attributeAdded);
		verify(backend3).deleteAttribute(currentEntityType3, attributeRemoved);
	}
}