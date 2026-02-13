package org.azdtasks.core.types;

/*
----------------------------------------------------------
	GENERATED FILE, should be edited to suit the purpose.
----------------------------------------------------------
*/


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
//import org.azd.abstractions.serializer.SerializableEntity;
import org.azd.abstractions.serializer.SerializableEntity;
import org.azd.common.types.ReferenceLinks;


/**
 * 
**/
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdentityRef  extends SerializableEntity {
	/**
 	* This field contains zero or more interesting links about the graph subject. These links may be invoked to obtain additional relationships or more detailed information about this graph subject. 
	**/
	@JsonProperty("_links")
	private ReferenceLinks _links;
	/**
 	* The descriptor is the primary way to reference the graph subject while the system is running. This field will uniquely identify the same graph subject across both Accounts and Organizations. 
	**/
	@JsonProperty("descriptor")
	private String descriptor;
	/**
 	* Deprecated - Can be retrieved by querying the Graph user referenced in the "self" entry of the IdentityRef "_links" dictionary 
	**/
	@JsonProperty("directoryAlias")
	private String directoryAlias;
	/**
 	* This is the non-unique display name of the graph subject. To change this field, you must alter its value in the source provider. 
	**/
	@JsonProperty("displayName")
	private String displayName;
	@JsonProperty("id")
	private String id;
	/**
 	* Deprecated - Available in the "avatar" entry of the IdentityRef "_links" dictionary 
	**/
	@JsonProperty("imageUrl")
	private String imageUrl;
	/**
 	* Deprecated - Can be retrieved by querying the Graph membership state referenced in the "membershipState" entry of the GraphUser "_links" dictionary 
	**/
	@JsonProperty("inactive")
	private boolean inactive;
	/**
 	* Deprecated - Can be inferred from the subject type of the descriptor (Descriptor.IsAadUserType/Descriptor.IsAadGroupType) 
	**/
	@JsonProperty("isAadIdentity")
	private boolean isAadIdentity;
	/**
 	* Deprecated - Can be inferred from the subject type of the descriptor (Descriptor.IsGroupType) 
	**/
	@JsonProperty("isContainer")
	private boolean isContainer;
	@JsonProperty("isDeletedInOrigin")
	private boolean isDeletedInOrigin;
	/**
 	* Deprecated - not in use in most preexisting implementations of ToIdentityRef 
	**/
	@JsonProperty("profileUrl")
	private String profileUrl;
	/**
 	* Deprecated - use Domain+PrincipalName instead 
	**/
	@JsonProperty("uniqueName")
	private String uniqueName;
	/**
 	* This url is the full route to the source resource of this graph subject. 
	**/
	@JsonProperty("url")
	private String url;

	public ReferenceLinks get_links() { return _links; }

	public void set_links(ReferenceLinks _links) { this._links = _links; }

	public String getDescriptor() { return descriptor; }

	public void setDescriptor(String descriptor) { this.descriptor = descriptor; }

	public String getDirectoryAlias() { return directoryAlias; }

	public void setDirectoryAlias(String directoryAlias) { this.directoryAlias = directoryAlias; }

	public String getDisplayName() { return displayName; }

	public void setDisplayName(String displayName) { this.displayName = displayName; }

	public String getId() { return id; }

	public void setId(String id) { this.id = id; }

	public String getImageUrl() { return imageUrl; }

	public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

	public Boolean getInactive() { return inactive; }

	public void setInactive(Boolean inactive) { this.inactive = inactive; }

	public Boolean getIsAadIdentity() { return isAadIdentity; }

	public void setIsAadIdentity(Boolean isAadIdentity) { this.isAadIdentity = isAadIdentity; }

	public Boolean getIsContainer() { return isContainer; }

	public void setIsContainer(Boolean isContainer) { this.isContainer = isContainer; }

	public Boolean getIsDeletedInOrigin() { return isDeletedInOrigin; }

	public void setIsDeletedInOrigin(Boolean isDeletedInOrigin) { this.isDeletedInOrigin = isDeletedInOrigin; }

	public String getProfileUrl() { return profileUrl; }

	public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }

	public String getUniqueName() { return uniqueName; }

	public void setUniqueName(String uniqueName) { this.uniqueName = uniqueName; }

	public String getUrl() { return url; }

	public void setUrl(String url) { this.url = url; }
}
