package no.mnemonic.act.platform.dao.api;

import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.criteria.ObjectStatisticsCriteria;
import no.mnemonic.act.platform.dao.api.record.FactAclEntryRecord;
import no.mnemonic.act.platform.dao.api.record.FactCommentRecord;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.api.result.ObjectStatisticsContainer;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;

import java.util.Optional;
import java.util.UUID;

/**
 * Interface for handling Objects and Facts. It acts as an abstraction of the underlying databases and should be used
 * instead of direct access to the Cassandra and ElasticSearch managers. This interface does not cover meta data such
 * as ObjectTypes and FactTypes. Meta data should be handled directly through the Cassandra managers.
 */
public interface ObjectFactDao {

  /**
   * Fetch an Object by its UUID. Returns NULL if the Object cannot be found.
   *
   * @param id UUID of Object
   * @return Object identified by its UUID
   */
  default ObjectRecord getObject(UUID id) {
    throw new UnsupportedOperationException();
  }

  /**
   * Fetch an Object by its type and value. Returns NULL if the Object cannot be found.
   *
   * @param type  Type of Object
   * @param value Value of Object
   * @return Object identified by its type and value
   */
  default ObjectRecord getObject(String type, String value) {
    throw new UnsupportedOperationException();
  }

  /**
   * Store a new Object. The caller must ensure that the same Object (by UUID and type/value) is not stored twice.
   *
   * @param object Object to store
   * @return Stored Object
   */
  default ObjectRecord storeObject(ObjectRecord object) {
    throw new UnsupportedOperationException();
  }

  /**
   * Calculate statistics about Facts bound to one or more Objects.
   *
   * @param criteria Criteria to calculate statistics
   * @return Container holding the calculated statistics
   */
  default ObjectStatisticsContainer calculateObjectStatistics(ObjectStatisticsCriteria criteria) {
    throw new UnsupportedOperationException();
  }

  /**
   * Search for Objects based on a search criteria.
   *
   * @param criteria Criteria to filter returned Objects
   * @return Container holding the search result
   */
  default ResultContainer<ObjectRecord> searchObjects(FactSearchCriteria criteria) {
    throw new UnsupportedOperationException();
  }

  /**
   * Fetch a Fact by its UUID. Returns NULL if the Fact cannot be found.
   *
   * @param id UUID of Fact
   * @return Fact identified by its UUID
   */
  default FactRecord getFact(UUID id) {
    throw new UnsupportedOperationException();
  }

  /**
   * Store a new Fact. The caller must ensure that the same Fact (by UUID and logically) is not stored twice. Use
   * {@link #retrieveExistingFact(FactRecord)} to check if a Fact already exists and {@link #refreshFact(FactRecord)}
   * to refresh an existing Fact.
   * <p>
   * If the supplied {@link FactRecord} contains an ACL or comments the method will store these records as well.
   *
   * @param fact Fact to store
   * @return Stored Fact
   */
  default FactRecord storeFact(FactRecord fact) {
    throw new UnsupportedOperationException();
  }

  /**
   * Refresh an existing Fact, i.e. update its lastSeenTimestamp. The caller must ensure that the Fact exists.
   * <p>
   * If the supplied {@link FactRecord} contains an ACL or comments the method will store any new records. Existing
   * records will not be updated.
   *
   * @param fact Fact to refresh
   * @return Refreshed Fact
   */
  default FactRecord refreshFact(FactRecord fact) {
    throw new UnsupportedOperationException();
  }

  /**
   * Mark an existing Fact as retracted. The caller must ensure that the Fact exists.
   * <p>
   * If the supplied {@link FactRecord} contains an ACL or comments the method will store any new records. Existing
   * records will not be updated.
   *
   * @param fact Fact to retract
   * @return Retracted Fact
   */
  default FactRecord retractFact(FactRecord fact) {
    throw new UnsupportedOperationException();
  }

  /**
   * Retrieve an existing Fact which is logically the same as the supplied {@link FactRecord}.
   *
   * @param fact Fact to check for existence
   * @return Existing Fact wrapped inside an {@link Optional}, or an empty {@link Optional} if non exist
   */
  default Optional<FactRecord> retrieveExistingFact(FactRecord fact) {
    throw new UnsupportedOperationException();
  }

  /**
   * Search for Facts based on a search criteria.
   *
   * @param criteria Criteria to filter returned Facts
   * @return Container holding the search result
   */
  default ResultContainer<FactRecord> searchFacts(FactSearchCriteria criteria) {
    throw new UnsupportedOperationException();
  }

  /**
   * Store a new ACL entry for an existing Fact. The caller must ensure that the Fact exists.
   *
   * @param fact     Fact owning the ACL entry
   * @param aclEntry ACL entry to store
   * @return Stored ACL entry
   */
  default FactAclEntryRecord storeFactAclEntry(FactRecord fact, FactAclEntryRecord aclEntry) {
    throw new UnsupportedOperationException();
  }

  /**
   * Store a new comment for an existing Fact. The caller must ensure that the Fact exists.
   *
   * @param fact    Fact owning the comment
   * @param comment Comment to store
   * @return Stored comment
   */
  default FactCommentRecord storeFactComment(FactRecord fact, FactCommentRecord comment) {
    throw new UnsupportedOperationException();
  }

  /**
   * Retrieve meta Facts bound to another Fact (identified by its UUID).
   *
   * @param id UUID of Fact
   * @return Container holding the resolved meta Facts
   */
  default ResultContainer<FactRecord> retrieveMetaFacts(UUID id) {
    throw new UnsupportedOperationException();
  }
}
