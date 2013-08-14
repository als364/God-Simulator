/**
 * material_lambertian.fp
 * 
 * Fragment shader shader which writes material information needed for Lambertian shading to
 * the gbuffer.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488)
 * @date 2012-03-27
 */

/* ID of Lambertian material, so the lighting shader knows what material
 * this pixel is. */
const int LAMBERTIAN_MATERIAL_ID = 2;

/* Material properties passed from the application. */
uniform vec3 DiffuseColor;

/* Textures and flags for whether they exist. */
uniform sampler2D DiffuseTexture;
uniform bool HasDiffuseTexture;

/* Fragment position, normal, and texcoord passed from the vertex shader. */
varying vec3 EyespacePosition;
varying vec3 EyespaceNormal;
varying vec2 TexCoord;

void main()
{
//SOLUTION
	/* Multiply diffuse color by diffuse texture, if we have one. */
	vec3 diffuse = DiffuseColor;
	if (HasDiffuseTexture)
	{
		diffuse *= texture2D(DiffuseTexture, TexCoord).rgb;
	}
	
	/* Store {diffuse, [unused], position, normal} into the gbuffer. */
	gl_FragData[0] = vec4(diffuse, float(LAMBERTIAN_MATERIAL_ID));
	gl_FragData[1] = vec4(0.0);
	gl_FragData[2] = vec4(EyespacePosition, 1.0);
	gl_FragData[3] = vec4(EyespaceNormal, 1.0);
//FILLIN
//	gl_FragData[0] = gl_FragData[1] = gl_FragData[2] = gl_FragData[3] = vec4(1.0);
//ENDSOLUTION
}
