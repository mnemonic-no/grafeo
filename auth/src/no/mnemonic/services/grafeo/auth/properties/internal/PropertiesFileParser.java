package no.mnemonic.services.grafeo.auth.properties.internal;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class to parse a properties file according to the specification.
 * <p>
 * Note: It will NOT ensure that referenced objects actually exist, i.e. if will NOT check if referenced members of
 * groups are also defined in the properties files. Clients will need to check the existence of referenced objects themselves.
 */
public class PropertiesFileParser {

  private static final Pattern FUNCTION_GROUP_PATTERN = Pattern.compile("function.(\\w+).members");
  private static final Pattern ORGANIZATION_NAME_PATTERN = Pattern.compile("organization.(\\d+).name");
  private static final Pattern SUBJECT_NAME_PATTERN = Pattern.compile("subject.(\\d+).name");
  private static final String ORGANIZATION_TYPE_KEY = "organization.{id}.type";
  private static final String ORGANIZATION_MEMBERS_KEY = "organization.{id}.members";
  private static final String SUBJECT_TYPE_KEY = "subject.{id}.type";
  private static final String SUBJECT_MEMBERS_KEY = "subject.{id}.members";
  private static final String SUBJECT_AFFILIATION_KEY = "subject.{id}.affiliation";
  private static final String SUBJECT_PERMISSION_KEY = "subject.{id}.permission.(\\d+)";

  private Set<PropertiesFunction> functions = new HashSet<>();
  private Set<PropertiesOrganization> organizations = new HashSet<>();
  private Set<PropertiesSubject> subjects = new HashSet<>();

  /**
   * Returns the Functions and FunctionGroups defined in the properties file after parsing it.
   *
   * @return Defined Functions and FunctionGroups
   */
  public Set<PropertiesFunction> getFunctions() {
    return functions;
  }

  /**
   * Returns the Organizations and OrganizationGroups defined in the properties file after parsing it.
   *
   * @return Defined Organizations and OrganizationGroups
   */
  public Set<PropertiesOrganization> getOrganizations() {
    return organizations;
  }

  /**
   * Returns the Subjects and SubjectGroups defined in the properties file after parsing it.
   *
   * @return Defined Subjects and SubjectGroups
   */
  public Set<PropertiesSubject> getSubjects() {
    return subjects;
  }

  /**
   * Parses a properties file according to the specification.
   *
   * @param file Path to properties file
   */
  public void parse(String file) {
    Properties properties = loadProperties(file);
    for (String key : properties.stringPropertyNames()) {
      if (matchFunctionGroup(properties, key)) continue;
      if (matchOrganization(properties, key)) continue;
      matchSubject(properties, key);
    }
  }

  private Properties loadProperties(String file) {
    if (!Files.isReadable(Paths.get(file))) throw new IllegalArgumentException("Cannot read properties file: " + file);

    try (InputStream is = new FileInputStream(file)) {
      Properties properties = new Properties();
      properties.load(is);
      return properties;
    } catch (IOException e) {
      throw new RuntimeException("Could not load properties file: " + file);
    }
  }

  private boolean matchFunctionGroup(Properties properties, String key) {
    // Test if 'key' contains a function group definition.
    // Only function groups are defined explicitly, functions are defined implicitly by referring to their names.
    Matcher matcher = FUNCTION_GROUP_PATTERN.matcher(key);
    if (!matcher.matches()) return false;

    // Extract function group name from key and parse members.
    functions.add(PropertiesFunctionGroup.builder()
            .setName(matcher.group(1))
            .setMembers(parseStringMembers(properties, key))
            .build()
    );
    return true;
  }

  private boolean matchOrganization(Properties properties, String key) {
    // Test if 'key' contains an organization definition.
    // An organization definition must have a name, otherwise all other entries will just be ignored.
    Matcher matcher = ORGANIZATION_NAME_PATTERN.matcher(key);
    if (!matcher.matches()) return false;

    // Extract internalID from key and name from value.
    long internalID = Long.parseUnsignedLong(matcher.group(1));
    String name = properties.getProperty(key, "").trim();
    boolean isGroup = isGroup(properties, internalID, ORGANIZATION_TYPE_KEY);

    PropertiesOrganization.Builder builder = isGroup ? PropertiesOrganizationGroup.builder() : PropertiesOrganization.builder();
    if (isGroup) {
      PropertiesOrganizationGroup.Builder.class.cast(builder).setMembers(parseNumericMembers(properties, internalID, ORGANIZATION_MEMBERS_KEY));
    }

    organizations.add(builder
            .setInternalID(internalID)
            .setName(name)
            .build()
    );
    return true;
  }

  private boolean matchSubject(Properties properties, String key) {
    // Test if 'key' contains a subject definition.
    // A subject definition must have a name, otherwise all other entries will just be ignored.
    Matcher matcher = SUBJECT_NAME_PATTERN.matcher(key);
    if (!matcher.matches()) return false;

    // Extract internalID from key and name from value.
    long internalID = Long.parseUnsignedLong(matcher.group(1));
    String name = properties.getProperty(key, "").trim();
    Long affiliation = parseNumericId(getPropertyForID(properties, internalID, SUBJECT_AFFILIATION_KEY));
    boolean isGroup = isGroup(properties, internalID, SUBJECT_TYPE_KEY);

    PropertiesSubject.Builder builder = isGroup ? PropertiesSubjectGroup.builder() : PropertiesSubject.builder();
    if (isGroup) {
      PropertiesSubjectGroup.Builder.class.cast(builder).setMembers(parseNumericMembers(properties, internalID, SUBJECT_MEMBERS_KEY));
    }

    subjects.add(builder
            .setInternalID(internalID)
            .setName(name)
            .setAffiliation(ObjectUtils.ifNull(affiliation, 0L))
            .setPermissions(parseSubjectPermissions(properties, internalID))
            .build()
    );
    return true;
  }

  private boolean isGroup(Properties properties, long id, String typeKey) {
    String type = getPropertyForID(properties, id, typeKey);
    return !StringUtils.isBlank(type) && "group".equals(type.trim());
  }

  private Set<Long> parseNumericMembers(Properties properties, long id, String membersKey) {
    Set<Long> members = new HashSet<>();

    // Members are comma-separated internalIDs.
    for (String member : getPropertyForID(properties, id, membersKey).split(",")) {
      if (StringUtils.isBlank(member)) continue;
      Long memberID = parseNumericId(member);
      if (memberID != null) {
        // Ignore entries which aren't a valid ID.
        members.add(memberID);
      }
    }

    return members;
  }

  private Set<String> parseStringMembers(Properties properties, String key) {
    Set<String> members = new HashSet<>();

    // Members are comma-separated function (group) names.
    for (String member : properties.getProperty(key, "").split(",")) {
      if (StringUtils.isBlank(member)) continue;
      members.add(member.trim());
    }

    return members;
  }

  private Map<Long, Set<String>> parseSubjectPermissions(Properties properties, long subjectID) {
    Map<Long, Set<String>> permissions = new HashMap<>();

    Pattern permissionPattern = Pattern.compile(SUBJECT_PERMISSION_KEY.replace("{id}", String.valueOf(subjectID)));
    for (String key : properties.stringPropertyNames()) {
      Matcher matcher = permissionPattern.matcher(key);
      if (!matcher.matches()) continue;
      // Extract organizationID from key and parse function (group) names.
      permissions.put(Long.parseUnsignedLong(matcher.group(1)), parseStringMembers(properties, key));
    }

    return permissions;
  }

  private String getPropertyForID(Properties properties, long id, String key) {
    return properties.getProperty(key.replace("{id}", String.valueOf(id)), "");
  }

  private Long parseNumericId(String id) {
    try {
      return Long.parseUnsignedLong(id.trim());
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

}
