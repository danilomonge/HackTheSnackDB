package de.pecus.api.entities;

/******************** SECCION IMPORTS ***************************************/
import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * A food product (e.g. "PEANUT BUTTER") whose recipe is reverse-engineered.
 * Maps to the PRODUCT table.
 */
@Entity
@Table(name = "PRODUCT")
public class ProductDO extends AuditBase<Long> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5382607608047169433L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="PK_ID")
	private Long id;
	@Column(name =  "DX_NAME" )
	private String name;
	@Column(name =  "DX_DESCRIPTION" )
	private String descripcion;
	
	/****************************  RELACION 1..N ******************************/
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "FK_BRAND_ID" , referencedColumnName = "PK_ID")
	private BrandDO brand;

	/****************************  RELACION 1..N ******************************/
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "FK_CATEGORY_ID" , referencedColumnName = "PK_ID")
	private CategoryDO category;

	/****************************  RELACION 1..N ******************************/
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "FK_SUBCATEGORY_ID" , referencedColumnName = "PK_ID")
	private SubCategoryDO subCategory;

	public ProductDO(){

	}

	public void finalize() throws Throwable {

	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public BrandDO getBrand() {
		return brand;
	}

	public void setBrand(BrandDO brand) {
		this.brand = brand;
	}

	public CategoryDO getCategory() {
		return category;
	}

	public void setCategory(CategoryDO category) {
		this.category = category;
	}

	public SubCategoryDO getSubCategory() {
		return subCategory;
	}

	public void setSubCategory(SubCategoryDO subCategory) {
		this.subCategory = subCategory;
	}
}