package com.entersekt.branding.entity;

/**
 * Following naming and access conventions for simple JSON serialise and deserialise
 * 
 */
public class Branding {

	public static final String TAG_SUBMIT_BUTTON_BACKGROUND_COLOUR = "<submit_button_background_colour>";
	public static final String TAG_TABLE_HEADING_BACKGROUND_COLOUR = "<table_heading_background_colour>";
	public static final String TAG_BODY_BACKGROUND_COLOUR = "<body_background_colour>";
	public static final String TAG_LOGO_BACKGROUND_SIZE = "<logo_background_size>";
	public static final String TAG__LOGO_HEIGHT = "<logo_height>";
	public static final String TAG_LOGO_BACKGROUND_COLOUR = "<logo_background_colour>";
	public static final String TAG_LOGO_IMAGE_NAME = "<logo_image_name>";

	public Branding(String logo_image_name, String logo_background_colour, int logo_height,
			int logo_background_size_width, int logo_background_size_height, String body_background_colour,
			String table_heading_background_colour, String submit_button_background_colour) {
		super();
		this.logo_image_name = logo_image_name;
		this.logo_background_colour = logo_background_colour;
		this.logo_height = logo_height;
		this.logo_background_size_width = logo_background_size_width;
		this.logo_background_size_height = logo_background_size_height;
		this.body_background_colour = body_background_colour;
		this.table_heading_background_colour = table_heading_background_colour;
		this.submit_button_background_colour = submit_button_background_colour;
	}

	public Branding() {
		super();
		// json serialiser requires
	}

	public String logo_image_name;
	public String logo_background_colour;
	public int logo_height;
	public int logo_background_size_width;
	public int logo_background_size_height;

	public String body_background_colour;
	public String table_heading_background_colour;
	public String submit_button_background_colour;
	public static final String ENTERSEKT = "entersekt";

	public String applyToTemplate(String template) {
		String logoSize = "";
		if (logo_background_size_width != 0) {
			logoSize = "background-size: " + logo_background_size_width + "px " + logo_background_size_height + "px;";
		}
		return template.replace(TAG_LOGO_IMAGE_NAME, logo_image_name)
				.replace(TAG_LOGO_BACKGROUND_COLOUR, logo_background_colour)
				.replace(TAG__LOGO_HEIGHT, logo_height + "").replace(TAG_LOGO_BACKGROUND_SIZE, logoSize)
				.replace(TAG_BODY_BACKGROUND_COLOUR, body_background_colour)
				.replace(TAG_TABLE_HEADING_BACKGROUND_COLOUR, table_heading_background_colour)
				.replace(TAG_SUBMIT_BUTTON_BACKGROUND_COLOUR, submit_button_background_colour);
	}

}
