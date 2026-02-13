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

import java.util.Date;
import java.util.List;


/**
 * Comment on a Work Item.
**/
@JsonIgnoreProperties(ignoreUnknown = true)
public class Comment  extends SerializableEntity {
	/**
 	* Link references to related REST resources. 
	**/
	@JsonProperty("_links")
	private ReferenceLinks _links;
	/**
 	* IdentityRef of the creator of the comment. 
	**/
	@JsonProperty("createdBy")
	private IdentityRef createdBy;
	/**
 	* The creation date of the comment. 
	**/
	@JsonProperty("createdDate")
	private Date createdDate;
	/**
 	* Effective Date/time value for adding the comment. Can be optionally different from CreatedDate. 
	**/
	@JsonProperty("createdOnBehalfDate")
	private Date createdOnBehalfDate;
	/**
 	* Identity on whose behalf this comment has been added. Can be optionally different from CreatedBy. 
	**/
	@JsonProperty("createdOnBehalfOf")
	private IdentityRef createdOnBehalfOf;
	/**
 	* Represents the possible types for the comment format. 
	**/
	@JsonProperty("format")
	private CommentFormat format;
	/**
 	* The id assigned to the comment. 
	**/
	@JsonProperty("id")
	private Long id;
	/**
 	* Indicates if the comment has been deleted. 
	**/
	@JsonProperty("isDeleted")
	private boolean isDeleted;
	/**
 	* The mentions of the comment. 
	**/
	@JsonProperty("mentions")
	private List<CommentMention> mentions;
	/**
 	* IdentityRef of the user who last modified the comment. 
	**/
	@JsonProperty("modifiedBy")
	private IdentityRef modifiedBy;
	/**
 	* The last modification date of the comment. 
	**/
	@JsonProperty("modifiedDate")
	private Date modifiedDate;
	/**
 	* The reactions of the comment. 
	**/
	@JsonProperty("reactions")
	private List<org.azdtasks.core.types.CommentReaction> reactions;
	/**
 	* The text of the comment in HTML format. 
	**/
	@JsonProperty("renderedText")
	private String renderedText;
	/**
 	* The text of the comment. 
	**/
	@JsonProperty("text")
	private String text;
	/**
 	* REST URL for the resource. 
	**/
	@JsonProperty("url")
	private String url;
	/**
 	* The current version of the comment. 
	**/
	@JsonProperty("version")
	private Long version;
	/**
 	* The id of the work item this comment belongs to. 
	**/
	@JsonProperty("workItemId")
	private Long workItemId;

	public ReferenceLinks get_links() { return _links; }

	public void set_links(ReferenceLinks _links) { this._links = _links; }

	public IdentityRef getCreatedBy() { return createdBy; }

	public void setCreatedBy(IdentityRef createdBy) { this.createdBy = createdBy; }

	public Date getCreatedDate() { return createdDate; }

	public void setCreatedDate(Date  createdDate) { this.createdDate = createdDate; }

	public Date getCreatedOnBehalfDate() { return createdOnBehalfDate; }

	public void setCreatedOnBehalfDate(Date createdOnBehalfDate) { this.createdOnBehalfDate = createdOnBehalfDate; }

	public IdentityRef getCreatedOnBehalfOf() { return createdOnBehalfOf; }

	public void setCreatedOnBehalfOf(IdentityRef createdOnBehalfOf) { this.createdOnBehalfOf = createdOnBehalfOf; }

	public CommentFormat getFormat() { return format; }

	public void setFormat(CommentFormat format) { this.format = format; }

	public Long getId() { return id; }

	public void setId(Long id) { this.id = id; }

	public Boolean getIsDeleted() { return isDeleted; }

	public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

	public List<CommentMention> getMentions() { return mentions; }

	public void setMentions(List<CommentMention> mentions) { this.mentions = mentions; }

	public IdentityRef getModifiedBy() { return modifiedBy; }

	public void setModifiedBy(IdentityRef modifiedBy) { this.modifiedBy = modifiedBy; }

	public Date getModifiedDate() { return modifiedDate; }

	public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }

	public List<CommentReaction> getReactions() { return reactions; }

	public void setReactions(List<CommentReaction> reactions) { this.reactions = reactions; }

	public String getRenderedText() { return renderedText; }

	public void setRenderedText(String renderedText) { this.renderedText = renderedText; }

	public String getText() { return text; }

	public void setText(String text) { this.text = text; }

	public String getUrl() { return url; }

	public void setUrl(String url) { this.url = url; }

	public Long getVersion() { return version; }

	public void setVersion(Long version) { this.version = version; }

	public Long getWorkItemId() { return workItemId; }

	public void setWorkItemId(Long workItemId) { this.workItemId = workItemId; }
}
