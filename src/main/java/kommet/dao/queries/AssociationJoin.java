/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.queries;

import kommet.data.KommetException;
import kommet.data.Type;

/**
 * This class represents an SQL JOIN between main type table, association linking table and associated type table.
 * 
 * Association joins need to be implemented with special logic, because they are grouped using brackets so that the inner join
 * between the linking table and association type table have precedence over the left join with the main queried type.
 * 
 * Let's say we have main table A, linking table B, associated type table C and record sharing table D. Then calling getSQL() on
 * this class will produce the following code:
 * <ul>
 * <li>If sharing are applied: LEFT JOIN ((B INNER JOIN D ON ...) INNER JOIN (C INNER JOIN D ON ...) ON B.x = C.x) ON A.x = B.x</li>
 * <li>If no sharing are applied: LEFT JOIN (B INNER JOIN C ON B.x = C.x) ON A.x = B.x</li>
 * </ul>
 * 
 * @author Radek Krawiec
 * @created 25-03-2014
 */
public class AssociationJoin implements SqlJoinStructure
{
	private String linkingTable;
	private String baseTable;
	private String associatedTable;
	private String linkingTableAlias;
	private String baseTableAlias;
	private String associatedTableAlias;
	private String linkingTableSelfField;
	private String linkingTableForeignField;
	private JoinType joinType;
	private Type linkingType;
	private Type associatedType;
	
	public AssociationJoin(String linkingTable, String baseTable, String associatedTable, String linkingTableAlias, String baseTableAlias, String associatedTableAlias, String linkingTableSelfField, String linkingTableForeignField, Type linkingType, Type associatedType, JoinType joinType)
	{
		super();
		this.linkingTable = linkingTable;
		this.baseTable = baseTable;
		this.associatedTable = associatedTable;
		this.linkingTableAlias = linkingTableAlias;
		this.baseTableAlias = baseTableAlias;
		this.associatedTableAlias = associatedTableAlias;
		this.setLinkingTableForeignField(linkingTableForeignField);
		this.setLinkingTableSelfField(linkingTableSelfField);
		this.joinType = joinType;
		this.linkingType = linkingType;
		this.associatedType = associatedType;
	}
	
	public String getSQL (String ... sharingJoins) throws KommetException
	{
		StringBuilder sql = new StringBuilder(this.joinType == JoinType.INNER_JOIN ? "INNER JOIN (" : "LEFT JOIN (");
		
		if (sharingJoins == null || sharingJoins.length != 2)
		{
			throw new KommetException("Two sharing joins expected in AssociationJoin, but got " + (sharingJoins != null ? sharingJoins.length : " null list"));
		}
		
		String linkingTableSharingJoin = sharingJoins[0];
		String associatedTableSharingJoin = sharingJoins[1];
		
		if (linkingTableSharingJoin != null)
		{
			sql.append("(").append(this.linkingTable);
			sql.append(" AS \"").append(this.linkingTableAlias).append("\"");
			sql.append(linkingTableSharingJoin);
		}
		else
		{
			sql.append(this.linkingTable);
			sql.append(" AS \"").append(this.linkingTableAlias).append("\"");
		}
		
		sql.append(" INNER JOIN ");
		
		if (associatedTableSharingJoin != null)
		{
			sql.append("(").append(this.associatedTable);
			sql.append(" AS \"").append(this.associatedTableAlias).append("\"");
			sql.append(associatedTableSharingJoin);
		}
		else
		{
			sql.append(this.associatedTable);
			sql.append(" AS \"").append(this.associatedTableAlias).append("\"");
		}
		
		sql.append(" ON ");
		
		// enclose column names in quotes in case they contain reserved keywords e.g. "view"
		sql.append("\"").append(this.linkingTableAlias).append("\".\"").append(this.linkingTableForeignField);
		sql.append("\" = \"");
		sql.append(this.associatedTableAlias).append("\".\"kid\"");
		
		// close the outermost bracket
		sql.append(") ON \"").append(baseTableAlias).append("\".\"kid\" = \"").append(this.linkingTableAlias).append("\".\"").append(this.linkingTableSelfField).append("\"");
		
		return sql.toString();
	}

	public String getLinkingTable()
	{
		return linkingTable;
	}

	public void setLinkingTable(String linkingTable)
	{
		this.linkingTable = linkingTable;
	}

	public String getBaseTable()
	{
		return baseTable;
	}

	public void setBaseTable(String baseTable)
	{
		this.baseTable = baseTable;
	}

	public String getAssociatedTable()
	{
		return associatedTable;
	}

	public void setAssociatedTable(String associatedTable)
	{
		this.associatedTable = associatedTable;
	}

	public String getLinkingTableAlias()
	{
		return linkingTableAlias;
	}

	public void setLinkingTableAlias(String linkingTableAlias)
	{
		this.linkingTableAlias = linkingTableAlias;
	}

	public String getBaseTableAlias()
	{
		return baseTableAlias;
	}

	public void setBaseTableAlias(String baseTableAlias)
	{
		this.baseTableAlias = baseTableAlias;
	}

	public String getAssociatedTableAlias()
	{
		return associatedTableAlias;
	}

	public void setAssociatedTableAlias(String associatedTableAlias)
	{
		this.associatedTableAlias = associatedTableAlias;
	}

	public void setLinkingTableSelfField(String linkingTableSelfField)
	{
		this.linkingTableSelfField = linkingTableSelfField;
	}

	public String getLinkingTableSelfField()
	{
		return linkingTableSelfField;
	}

	public void setLinkingTableForeignField(String linkingTableForeignField)
	{
		this.linkingTableForeignField = linkingTableForeignField;
	}

	public String getLinkingTableForeignField()
	{
		return linkingTableForeignField;
	}

	public void setLinkingType(Type linkingType)
	{
		this.linkingType = linkingType;
	}

	public Type getLinkingType()
	{
		return linkingType;
	}

	public void setAssociatedType(Type associationType)
	{
		this.associatedType = associationType;
	}

	public Type getAssociatedType()
	{
		return associatedType;
	}
}