/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.voronoi;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Line2D;
import java.util.*;
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
	private static final int scale = 200;
	private static final int n = 2000;
	
	private JPanel panel;
	private List<Vector2d> points;
	private List<Edge> edges;

	public DisplayVoronoiDiagram() throws HeadlessException {
		setTitle("Voronoi Diagram");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		panel = new JPanel() {

			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				paintPanel(g);
			}
			
		};
		setLayout(new BorderLayout());
		add(panel, BorderLayout.CENTER);
		panel.setPreferredSize(new Dimension(w, h));
		pack();
		generateVoronoi();
		addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					generateVoronoi();
				}
			}
			
		});
	}
	
	private void generateVoronoi() {
		Random rand = new Random();
		Voronoi voronoi = new Voronoi();
		Set<Vector2d> pointSet = new HashSet<>();
		for (int i=0; i<n; ++i) {
			float x = (rand.nextFloat() * w * scale);
			float y = (rand.nextFloat() * h * scale);
			pointSet.add(new Vector2d(x, y));
		}
		points = new ArrayList<>(n);
		points.addAll(pointSet);
		
		long time1 = System.currentTimeMillis();
		edges = voronoi.getEdges(points, w*scale, h*scale);
		edges = Voronoi.closeEdges(edges, w*scale, h*scale);
		//relax n times
		int m = 1;
		for (int i=0; i<m; ++i) {
			points = VoronoiUtils.generateRelaxedSites(points, edges);
			edges = voronoi.getEdges(points, w*scale, h*scale);
			edges = Voronoi.closeEdges(edges, w*scale, h*scale);
		}
		long time2 = System.currentTimeMillis();
		
		for (Iterator<Edge> it = edges.iterator(); it.hasNext(); ) {
			Edge e = it.next();
			Vector2d v = new Vector2d(e.getStart().x - e.getEnd().x, e.getEnd().y - e.getEnd().y);
			if (v.length() > 0.05 * w * scale) {
				System.out.println("long edge: "+e);
//				it.remove();
			}
		}
		
		System.out.println("edges: ("+edges.size()+")");
//		for (Edge e : edges) {
//			System.out.println(" "+e.start+" -- "+e.end);
//		}
		System.out.println("time to compute: "+(time2-time1)+" msec");
		panel.repaint();
	}
	
	private void paintPanel(Graphics g) {
		Graphics2D G = (Graphics2D) g;
		G.translate(10, 10);
		G.scale(0.95, 0.95);
		G.setStroke(new BasicStroke(3));
		G.setPaint(Color.RED);
		for (Edge e : edges) {
			double x1 = e.start.x / scale;
			double y1 = e.start.y / scale;
			double x2 = e.end.x / scale;
			double y2 = e.end.y / scale;
			
			Vector2d v = new Vector2d(e.getStart().x - e.getEnd().x, e.getEnd().y - e.getEnd().y);
			if (v.length() > 0.05 * w * scale) {
				G.setPaint(Color.BLUE);
			} else {
				G.setPaint(Color.RED);
			}
			
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
			Line2D l = new Line2D.Double(x1, y1, x2, y2);
			G.draw(l);
			if (e.right != null) {
				double x3 = e.right.x / scale;
				double y3 = e.right.y / scale;
				l.setLine(x1, y1, x3, y3);
				G.draw(l);
			}
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
