/**
 * silhouette.fp
 * 
 * Fragment shader for calculating silhouette edges as described in Decaudin's 1996 paper.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488)
 * @date 2012-04-01
 */

uniform sampler2DRect PositionBuffer;
uniform sampler2DRect NormalBuffer;

/**
 * Samples from position and normal buffer and returns (nx, ny, nz, depth) packed into one vec4.
 */
vec4 sample(vec2 coord)
{
	return vec4(texture2DRect(NormalBuffer, coord).xyz, texture2DRect(PositionBuffer, coord).z);
}

void main()
{
	/* Take a 3x3 sample of positions and normals and perform edge/crease 
	 * estimation as in [Decaudin96]. */
//SOLUTION
	/* Take 3x3 square of samples (using the naming scheme in Decaudin section 2.3.1):
	 *    -------------
	 *    | A | B | C |
	 *    -------------
	 *    | D | x | E |
	 *    -------------
	 *    | F | G | H |
	 *    -------------
	 */
	vec4 x = sample(gl_FragCoord.xy);
	vec4 A = sample(gl_FragCoord.xy + vec2(-1.0,  1.0));
	vec4 B = sample(gl_FragCoord.xy + vec2( 0.0,  1.0));
	vec4 C = sample(gl_FragCoord.xy + vec2( 1.0,  1.0));
	vec4 D = sample(gl_FragCoord.xy + vec2(-1.0,  0.0));
	vec4 E = sample(gl_FragCoord.xy + vec2( 1.0,  0.0));
	vec4 F = sample(gl_FragCoord.xy + vec2(-1.0, -1.0));
	vec4 G = sample(gl_FragCoord.xy + vec2( 0.0, -1.0));
	vec4 H = sample(gl_FragCoord.xy + vec2( 1.0, -1.0));
	
	/* These samples are (nx, ny, nz, depth), but we want to do the same operation
	 * to all of them. */	
	vec4 g = (abs(A - x) + 2.0 * abs(B - x) + abs(C - x) + 
	          2.0 * abs(D - x) + 2.0 * abs(E - x) + 
	          abs(F - x) + 2.0 * abs(G - x) + abs(H - x)) / 8.0;
//FILLIN
//	vec4 g = vec4(0.0);
//ENDSOLUTION

	/* Output silhouette estimates for use by the final post-processing stage. */
	gl_FragColor = g;
}
