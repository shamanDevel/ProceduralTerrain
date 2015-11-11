/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.voronoi;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.vecmath.Vector2d;
import org.shaman.terrain.polygonal.VoronoiUtils;

/**
 *
 * @author Sebastian Weiss
 */
public class DisplayVoronoiDiagram extends JFrame {
	private static final int w = 800;
	private static final int h = 800;
	private static final int scale = 100;
	private static final int n = 1000;
	
	private JPanel panel;
	private List<Vector2d> points;
	private List<Edge> edges;

	public DisplayVoronoiDiagram() throws HeadlessException {
		setTitle("Voronoi Diagram");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		panel = new JPanel() {

			@Override
			public void paintComponent(Graphics g) {
				paintPanel(g);
			}
			
		};
		setLayout(new BorderLayout());
		add(panel, BorderLayout.CENTER);
		panel.setPreferredSize(new Dimension(w, h));
		pack();
		generateVoronoi();
	}
	
	private void generateVoronoi() {
		Random rand = new Random();
		Voronoi voronoi = new Voronoi();
		points = new ArrayList<>(n);
		for (int i=0; i<n; ++i) {
			float x = rand.nextFloat() * w * scale;
			float y = rand.nextFloat() * h * scale;
			points.add(new Vector2d(x, y));
		}
		
		long time1 = System.currentTimeMillis();
		edges = voronoi.getEdges(points, w*scale, h*scale);
		edges = Voronoi.closeEdges(edges, w*scale, h*scale);
		//relax n times
		int m = 3;
		for (int i=0; i<m; ++i) {
			points = VoronoiUtils.generateRelaxedSites(points, edges);
			edges = voronoi.getEdges(points, w*scale, h*scale);
			edges = Voronoi.closeEdges(edges, w*scale, h*scale);
		}
		long time2 = System.currentTimeMillis();
		System.out.println("edges: ("+edges.size()+")");
		
		for (Edge e : edges) {
			System.out.println(" "+e.start+" -- "+e.end);
		}
		System.out.println("time to compute: "+(time2-time1)+" msec");
		panel.repaint();
	}
	
	private void paintPanel(Graphics g) {
		Graphics2D G = (Graphics2D) g;
		G.setStroke(new BasicStroke(3));
		G.setPaint(Color.RED);
		for (Edge e : edges) {
			double x1 = e.start.x / scale;
			double y1 = e.start.y / scale;
			double x2 = e.end.x / scale;
			double y2 = e.end.y / scale;
			Line2D l = new Line2D.Double(x1, y1, x2, y2);
			G.draw(l);
		}
		G.setPaint(Color.GRAY);
		G.setStroke(new BasicStroke(1));
		for (Edge e : edges) {
			double x1 = (e.start.x + e.end.x) / 2 / scale;
			double y1 = (e.start.y + e.end.y) / 2 / scale;
			double x2 = e.left.x / scale;
			double y2 = e.left.y / scale;
			double x3 = e.right.x / scale;
			double y3 = e.right.y / scale;
			Line2D l = new Line2D.Double(x1, y1, x2, y2);
			G.draw(l);
			l.setLine(x1, y1, x3, y3);
			G.draw(l);
		}
		G.setPaint(Color.BLACK);
		for (Vector2d p : points) {
			int x = (int) (p.x / scale);
			int y = (int) (p.y / scale);
			G.fillOval(x-1, y-1, 3, 3);
		}
	}
	
	
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		DisplayVoronoiDiagram f = new DisplayVoronoiDiagram();
		f.setVisible(true);
		f.setLocationRelativeTo(null);
	}
	
}
