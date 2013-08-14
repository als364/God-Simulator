/**
 * material_unshaded.fp
 * 
 * Fragment shader shader which writes material information needed for unshaded (constant color) 
 * shading to the gbuffer.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488)
 * @date 2012-03-27
 */

/* ID of unshaded material, so the lighting shader knows what material
 * this pixel is. */
const int UNSHADED_MATERIAL_ID = 1;

/* Material properties passed from the application. */
uniform vec3 Color;

/* Fragment position and normal passed from the vertex shader. */
varying vec3 EyespacePosition;
varying vec3 EyespaceNormal;

void main()
{
	/* Store {diffuse, [unused], position, normal} into the gbuffer. Position and normal
	 * aren't used for shading, but they might be required by a post-processing effect, so
	 * we still have to write them out. */
	gl_FragData[0] = vec4(Color, float(UNSHADED_MATERIAL_ID));
	gl_FragData[1] = vec4(0.0);
	gl_FragData[2] = vec4(EyespacePosition, 1.0);
	gl_FragData[3] = vec4(EyespaceNormal, 1.0);
}
