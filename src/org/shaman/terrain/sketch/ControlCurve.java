/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.sketch;

import com.jme3.math.Vector3f;
import java.util.Arrays;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Sebastian Weiss
 */
public class ControlCurve {
	private static final Logger LOG = Logger.getLogger(ControlCurve.class.getName());
	
	private ControlPoint[] points;
	private float[] times;

	public ControlCurve(ControlPoint[] points) {
		if (points.length < 2) {
			throw new IllegalArgumentException("At least two control points have to be specified");
		}
		this.points = points;
		//approximate geodesic sampling by the euclidian distance
		float[] distances = new float[points.length];
		distances[0] = 0;
		for (int i = 1; i < points.length; ++i) {
			distances[i] = new Vector3f(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y, points[i].height - points[i - 1].height).length();
			distances[i] += distances[i - 1];
		}
		times = new float[points.length];
		for (int i = 0; i < points.length; ++i) {
			times[i] = distances[i] / distances[points.length - 1];
		}
		LOG.info("Control points:\n" + StringUtils.join(points, '\n'));
		LOG.info("Distances: " + Arrays.toString(distances));
		LOG.info("Times: " + Arrays.toString(times));
	} //approximate geodesic sampling by the euclidian distance

	public ControlPoint[] getPoints() {
		return points;
	}

	public ControlPoint interpolate(float time) {
		int i;
		for (i = 1; i < points.length; ++i) {
			if (times[i - 1] <= time && times[i] >= time) {
				break;
			}
		}
		if (i == points.length) {
			return null; //outside of the bounds
		}
		ControlPoint p1 = points[i - 1];
		ControlPoint p2 = points[i];
		float t = (time - times[i - 1]) / (times[i] - times[i - 1]);
		//interpolate
		ControlPoint p = new ControlPoint();
		//position
		if (points.length == 2) {
			//linear interpolation of the position
			p.x = (1 - t) * p1.x + t * p2.x;
			p.y = (1 - t) * p1.y + t * p2.y;
			p.height = (1 - t) * p1.height + t * p2.height;
		} else if (i == 1) {
			//quadratic hermite
			Vector3f P0 = new Vector3f(p1.x, p1.y, p1.height);
			Vector3f P1 = new Vector3f(p2.x, p2.y, p2.height);
			Vector3f T1 = new Vector3f(points[i + 1].x - p1.x, points[i + 1].y - p1.y, points[i + 1].height - p1.height);
			T1.multLocal(0.5f);
			Vector3f P = quadraticHermite(P0, T1, P1, t);
			p.x = P.x;
			p.y = P.y;
			p.height = P.z;
		} else if (i == points.length - 1) {
			//quadratic hermite
			Vector3f P0 = new Vector3f(p1.x, p1.y, p1.height);
			Vector3f P1 = new Vector3f(p2.x, p2.y, p2.height);
			Vector3f T0 = new Vector3f(p2.x - points[i - 2].x, p2.y - points[i - 2].y, p2.height - points[i - 2].height);
			T0.multLocal(-0.5f);
			Vector3f P = quadraticHermite(P1, T0, P0, 1 - t);
			p.x = P.x;
			p.y = P.y;
			p.height = P.z;
		} else {
			Vector3f P0 = new Vector3f(p1.x, p1.y, p1.height);
			Vector3f P1 = new Vector3f(p2.x, p2.y, p2.height);
			Vector3f T0 = new Vector3f(p2.x - points[i - 2].x, p2.y - points[i - 2].y, p2.height - points[i - 2].height);
			Vector3f T1 = new Vector3f(points[i + 1].x - p1.x, points[i + 1].y - p1.y, points[i + 1].height - p1.height);
			T0.multLocal(0.5f);
			T1.multLocal(0.5f);
			Vector3f P = cubicHermite(P0, T0, P1, T1, t);
			p.x = P.x;
			p.y = P.y;
			p.height = P.z;
		}
		//all other properties are linearly interpolated
		p.plateau = (1 - t) * p1.plateau + t * p2.plateau;
		p.angle1 = (1 - t) * p1.angle1 + t * p2.angle1;
		p.extend1 = (1 - t) * p1.extend1 + t * p2.extend1;
		p.angle2 = (1 - t) * p1.angle2 + t * p2.angle2;
		p.extend2 = (1 - t) * p1.extend2 + t * p2.extend2;
		p.noiseAmplitude = (1 - t) * p1.noiseAmplitude + t * p2.noiseAmplitude;
		p.noiseRoughness = (1 - t) * p1.noiseRoughness + t * p2.noiseRoughness;
		return p;
	}

	/**
	 * Hermite interpolation of the points P0 to P1 at time t=0 to t=1 with
	 * the specified velocities T0 and T1.
	 *
	 * @param P0
	 * @param T0
	 * @param P1
	 * @param T1
	 * @param t
	 * @return
	 */
	private static Vector3f cubicHermite(Vector3f P0, Vector3f T0, Vector3f P1, Vector3f T1, float t) {
		float t2 = t * t;
		float t3 = t2 * t;
		Vector3f P = new Vector3f();
		P.scaleAdd(2 * t3 - 3 * t2 + 1, P0, P);
		P.scaleAdd(t3 - 2 * t2 + t, T0, P);
		P.scaleAdd(-2 * t3 + 3 * t2, P1, P);
		P.scaleAdd(t3 - t2, T1, P);
		return P;
	}

	/**
	 * A variation of Hermite where a velocity is given only for the first
	 * point. It interpolates P0 at t=0 and P1 at t=1. P(t) = (t^2 - 2*t +
	 * 1) * P0 + (-t^2 + 2*t) * P1 + (t^2 - t) * T0
	 *
	 * @param P0
	 * @param T0
	 * @param P1
	 * @param t
	 * @return
	 */
	private static Vector3f quadraticHermite(Vector3f P0, Vector3f T0, Vector3f P1, float t) {
		float t2 = t * t;
		Vector3f P = new Vector3f();
		P.scaleAdd(t2 - 2 * t + 1, P0, P);
		P.scaleAdd(-t2 + 2 * t, P1, P);
		P.scaleAdd(t2 - t, T0, P);
		return P;
	}
	
}
