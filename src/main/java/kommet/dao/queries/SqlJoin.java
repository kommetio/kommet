/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.queries;

import kommet.data.Type;

public class SqlJoin implements SqlJoinStructure
{
	private JoinType joinType = JoinType.LEFT_JOIN;
	private String leftTable;
	private String rightTable;
	private String leftColumn;
	private String rightColumn;
	private String leftTableAlias;
	private String rightTableAlias;
	private Type joinedType;

	public SqlJoin (JoinType joinType, String leftTable, String leftColumn, String leftTableAlias, String rightTable, String rightColumn, String rightTableAlias, Type joinedType)
	{
		this.joinType = joinType;
		this.leftColumn = leftColumn;
		this.leftTable = leftTable;
		this.rightColumn = rightColumn;
		this.rightTable = rightTable;
		this.leftTableAlias = leftTableAlias;
		this.rightTableAlias = rightTableAlias;
		this.joinedType = joinedType;
	}

	public JoinType getJoinType()
	{
		return joinType;
	}

	public void setLeftTable(String leftTable)
	{
		this.leftTable = leftTable;
	}

	public String getLeftTable()
	{
		return leftTable;
	}

	public void setRightTable(String rightTable)
	{
		this.rightTable = rightTable;
	}

	public String getRightTable()
	{
		return rightTable;
	}

	public void setLeftColumn(String leftColumn)
	{
		this.leftColumn = leftColumn;
	}

	public String getLeftColumn()
	{
		return leftColumn;
	}

	public void setRightColumn(String rightColumn)
	{
		this.rightColumn = rightColumn;
	}

	public String getRightColumn()
	{
		return rightColumn;
	}

	public void setLeftTableAlias(String leftTableAlias)
	{
		this.leftTableAlias = leftTableAlias;
	}

	public String getLeftTableAlias()
	{
		return leftTableAlias;
	}

	public void setRightTableAlias(String rightTableAlias)
	{
		this.rightTableAlias = rightTableAlias;
	}

	public String getRightTableAlias()
	{
		return rightTableAlias;
	}

	public String getSQL (String ... sharingJoins)
	{
		StringBuilder sql = new StringBuilder(this.joinType == JoinType.INNER_JOIN ? "INNER JOIN " : "LEFT JOIN ");
		
		if (sharingJoins != null && sharingJoins.length > 0)
		{
			sql.append("(").append(this.rightTable);
			sql.append(" AS \"").append(this.rightTableAlias).append("\"");
			sql.append(sharingJoins[0]);
		}
		else
		{
			sql.append(this.rightTable);
			sql.append(" AS \"").append(this.rightTableAlias).append("\"");
		}
		sql.append(" ON ");
		// enclose column names in quotes in case they contain reserved keywords e.g. "view"
		sql.append("\"").append(this.leftTableAlias).append("\".\"").append(this.leftColumn);
		sql.append("\" = \"");
		sql.append(this.rightTableAlias).append("\".\"").append(this.rightColumn).append("\"");
		
		return sql.toString();
	}

	public Type getJoinedType()
	{
		return joinedType;
	}
}