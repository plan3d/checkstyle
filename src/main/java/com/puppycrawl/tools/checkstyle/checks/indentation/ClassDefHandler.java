////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2021 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle.checks.indentation;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.TokenUtil;

/**
 * Handler for class definitions.
 *
 */
public class ClassDefHandler extends BlockParentHandler {

    /**
     * Construct an instance of this handler with the given indentation check,
     * abstract syntax tree, and parent handler.
     *
     * @param indentCheck   the indentation check
     * @param ast           the abstract syntax tree
     * @param parent        the parent handler
     */
    public ClassDefHandler(IndentationCheck indentCheck,
                           DetailAST ast,
                           AbstractExpressionHandler parent) {
        super(indentCheck, getHandlerName(ast), ast, parent);
    }

    @Override
    protected DetailAST getLeftCurly() {
        return getMainAst().findFirstToken(TokenTypes.OBJBLOCK)
            .findFirstToken(TokenTypes.LCURLY);
    }

    @Override
    protected DetailAST getRightCurly() {
        return getMainAst().findFirstToken(TokenTypes.OBJBLOCK)
            .findFirstToken(TokenTypes.RCURLY);
    }

    @Override
    protected DetailAST getTopLevelAst() {
        return null;
        // note: ident checked by hand in check indentation;
    }

    @Override
    protected DetailAST getListChild() {
        return getMainAst().findFirstToken(TokenTypes.OBJBLOCK);
    }

    @Override
    public void checkIndentation() {
        final DetailAST modifiers = getMainAst().findFirstToken(TokenTypes.MODIFIERS);
        if (modifiers.hasChildren()) {
            checkModifiers();
        }
        else {
            if (getMainAst().getType() != TokenTypes.ANNOTATION_DEF) {
                final DetailAST ident = getMainAst().findFirstToken(TokenTypes.IDENT);
                final int lineStart = getLineStart(ident);
                if (!getIndent().isAcceptable(lineStart)) {
                    logError(ident, "ident", lineStart);
                }
            }
        }
        if (getMainAst().getType() == TokenTypes.ANNOTATION_DEF) {
            final DetailAST atAst = getMainAst().findFirstToken(TokenTypes.AT);
            if (isOnStartOfLine(atAst)) {
                checkWrappingIndentation(getMainAst(), getListChild(), 0,
                        getIndent().getFirstIndentLevel(), false);
            }
        }
        else {
            checkWrappingIndentation(getMainAst(), getListChild());
        }
        if (getMainAst().getType() == TokenTypes.ENUM_DEF) {
            TokenUtil.forEachChild(getListChild(), TokenTypes.ENUM_CONSTANT_DEF,
                    this::checkEnumConstantDefIndentation);
        }
        super.checkIndentation();
    }

    /**
     * Checks enum constant definition.
     *
     * @param ast enum constat definition to examine
     */
    private void checkEnumConstantDefIndentation(DetailAST ast) {
        final DetailAstSet astSet = new DetailAstSet(getIndentCheck());
        findSubtreeAst(astSet, ast, true);
        final IndentLevel expectedIndent =
            new IndentLevel(getIndent(), getIndentCheck().getBasicOffset());
        // check first line
        final DetailAST firstLine = astSet.firstLine();
        if (isOnStartOfLine(firstLine)
                && !expectedIndent.isAcceptable(firstLine.getColumnNo())) {
            String subtypeName = "";
            if (firstLineIsAnnotation(firstLine)) {
                subtypeName = "annotation";
            }
            logError(firstLine, subtypeName, firstLine.getColumnNo(), expectedIndent);
        }
        // check rest of lines
        if (firstLine != astSet.getAst(astSet.lastLine())) {
            checkWrappingIndentation(firstLine,
                    firstLine.getLastChild(),
                    getIndentCheck().getLineWrappingIndentation(),
                    expectedIndent.getFirstIndentLevel(),
                    true);
        }
    }

    /**
     * Examines if first line of enum constant definition is an annotation.
     *
     * @param enumConstantDef Node to examine
     * @return true if first line is an annotation
     */
    private static boolean firstLineIsAnnotation(DetailAST enumConstantDef) {
        return enumConstantDef.getFirstChild().getFirstChild() != null;
    }

    @Override
    protected int[] getCheckedChildren() {
        return new int[] {
            TokenTypes.EXPR,
            TokenTypes.OBJBLOCK,
            TokenTypes.LITERAL_BREAK,
            TokenTypes.LITERAL_RETURN,
            TokenTypes.LITERAL_THROW,
            TokenTypes.LITERAL_CONTINUE,
        };
    }

    /**
     * Creates a handler name for this class according to ast type.
     *
     * @param ast the abstract syntax tree.
     * @return handler name for this class.
     */
    private static String getHandlerName(DetailAST ast) {
        final String name;
        final int tokenType = ast.getType();

        switch (tokenType) {
            case TokenTypes.CLASS_DEF:
                name = "class def";
                break;
            case TokenTypes.ENUM_DEF:
                name = "enum def";
                break;
            case TokenTypes.ANNOTATION_DEF:
                name = "annotation def";
                break;
            case TokenTypes.RECORD_DEF:
                name = "record def";
                break;
            default:
                name = "interface def";
        }

        return name;
    }

}
