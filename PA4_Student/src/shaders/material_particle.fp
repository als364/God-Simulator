/**
 * material_particle.fp
 * 
 * Fragment shader shader which forward-shades an (unshaded, alpha-blended) particle system.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488)
 * @date 2012-04-15
 */

/* Material properties passed from the application. */
uniform vec4 Color;

/* Textures and flags for whether they exist. */
uniform sampler2D Texture;
uniform bool HasTexture;

/* Texture coordinate passed from vertex shader. */
varying vec2 TexCoord;

void main()
{
	/* Multiply color by texture, if we have one. */
	vec4 color = Color;
	if (HasTexture)
	{	color = vec4(1.0);
		color *= texture2D(Texture, TexCoord);
	}

	/* Output premultiplied color and alpha to achieve regular alpha blending.  
	 * See `ParticleMaterial.bind()` for an explanation. */ 
	gl_FragColor = vec4(color.rgb * color.a, color.a);	
}
