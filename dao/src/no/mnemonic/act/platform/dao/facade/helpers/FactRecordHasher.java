package no.mnemonic.act.platform.dao.facade.helpers;

import com.google.common.hash.Hashing;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.ObjectUtils;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Class to calculate the hash value (SHA256) of a {@link FactRecord}. Facts which are logically considered to be the
 * same will produce the exact same hash value.
 * <p>
 * <u>Algorithm to calculate hash(fact)</u>:
 * <p>
 * Given a {@link FactRecord} with the following fields:
 * <ul>
 * <li>value</li>
 * <li>typeID</li>
 * <li>originID</li>
 * <li>organizationID</li>
 * <li>accessMode</li>
 * <li>confidence</li>
 * <li>inReferenceToID</li>
 * <li>sourceObjectID</li>
 * <li>destinationObjectID</li>
 * <li>isBidirectionalBinding</li>
 * </ul>
 * construct the following string
 * <p>
 * {@code typeID=$typeID;originID=$originID;organizationID=$organizationID;accessMode=$accessMode;confidence=$confidence;inReferenceToID=$inReferenceToID;sourceObjectID=$sourceObjectID;destinationObjectID=$destinationObjectID;isBidirectionalBinding=$isBidirectionalBinding;value=$value}
 * <p>
 * where null variables must be represented as "NULL" and confidence must be rounded to two decimal points.
 * If "value" is unset omit it, i.e. use "value=".
 * <p>
 * Calculate the hash value as SHA256($string).
 */
public class FactRecordHasher {

  private static final Logger LOGGER = Logging.getLogger(FactRecordHasher.class);
  private static final String NULL_REPLACEMENT = "NULL";

  private FactRecordHasher() {
  }

  /**
   * Calculate SHA256 of a {@link FactRecord}. See {@link FactRecordHasher} for details.
   *
   * @param record Fact to calculate SHA256
   * @return Calculated SHA256
   */
  public static String toHash(FactRecord record) {
    if (record == null) throw new IllegalArgumentException("'record' cannot be null!");

    String formatted = formatFactRecord(record);
    LOGGER.debug("Formatted FactRecord: " + formatted);

    String hashed = calculateSha256(formatted);
    LOGGER.debug("Hashed FactRecord: " + hashed);

    return hashed;
  }

  static String formatFactRecord(FactRecord record) {
    StringBuilder sb = new StringBuilder()
            .append("typeID=")
            .append(nullSafeString(record.getTypeID()))
            .append(";")
            .append("originID=")
            .append(nullSafeString(record.getOriginID()))
            .append(";")
            .append("organizationID=")
            .append(nullSafeString(record.getOrganizationID()))
            .append(";")
            .append("accessMode=")
            .append(nullSafeString(record.getAccessMode()))
            .append(";")
            .append("confidence=")
            .append(formatFloatingPoint(record.getConfidence()))
            .append(";")
            .append("inReferenceToID=")
            .append(nullSafeString(record.getInReferenceToID()))
            .append(";")
            .append("sourceObjectID=")
            .append(formatObjectRecord(record.getSourceObject()))
            .append(";")
            .append("destinationObjectID=")
            .append(formatObjectRecord(record.getDestinationObject()))
            .append(";")
            .append("isBidirectionalBinding=")
            .append(nullSafeString(record.isBidirectionalBinding()))
            .append(";")
            .append("value=");

    if (record.getValue() != null) {
      // If 'value' is unset omit it completely (especially don't use nullSafeString()).
      // Otherwise, an unset 'value' would be the same as 'value' set to 'NULL'.
      sb.append(record.getValue());
    }

    return sb.toString();
  }

  static String formatFloatingPoint(float number) {
    // Ensure locale-independent and constant formatting with values rounded to two decimal points.
    DecimalFormat format = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    format.setRoundingMode(RoundingMode.HALF_UP);
    return format.format(number);
  }

  private static String calculateSha256(String input) {
    return Hashing.sha256()
            .hashString(input, StandardCharsets.UTF_8)
            .toString();
  }

  private static String formatObjectRecord(ObjectRecord record) {
    return record == null ? NULL_REPLACEMENT : nullSafeString(record.getId());
  }

  private static String nullSafeString(Object object) {
    return ObjectUtils.ifNotNull(object, Object::toString, NULL_REPLACEMENT);
  }
}
